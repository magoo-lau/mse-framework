package vip.mseapp.service.system.notify;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import tools.jackson.databind.json.JsonMapper;
import vip.mseapp.api.notify.NotifySourceProvider;
import vip.mseapp.api.notify.RemoteNotifyChannel;
import vip.mseapp.api.notify.dto.NotifyChannelDescriptor;
import vip.mseapp.api.notify.dto.NotifySourceDescriptor;
import vip.mseapp.api.notify.dto.NotifyTypeDescriptor;
import vip.mseapp.core.constant.MseConstants;
import vip.mseapp.core.context.TenantContext;
import vip.mseapp.redis.config.MseRedisAutoConfiguration.LockOps;
import vip.mseapp.service.system.entity.SysNotifyChannel;
import vip.mseapp.service.system.entity.SysNotifySource;
import vip.mseapp.service.system.mapper.SysNotifyChannelMapper;
import vip.mseapp.service.system.mapper.SysNotifySourceMapper;

/**
 * 通知目录：渠道目录与业务来源目录的合并视图（本地 Bean 收集 + 登记源 DB 行）。
 *
 * <p>渠道目录：本地 {@link RemoteNotifyChannel} Bean（单体聚合渠道/框架内置站内信）与登记源
 * DB 行（dubbo 推送，tenant_id=0）合并——渠道对框架动态感知，新渠道上线框架与各业务零代码改动。
 *
 * <p>来源目录：本地 {@link NotifySourceProvider} Bean 与登记源 DB 行（sys_notify_source）合并，
 * 供路由配置页「业务来源/消息类型」下拉与保存校验。
 *
 * <p>登记接收（{@link #register} / {@link #registerSources}）幂等 upsert——按编码全局查
 * （runIgnore 跨租户）：不存在则新增登记源行；同一提供方重推仅在有变化时同步描述；编码已被
 * 其他提供组占用时跳过并告警，防多提供方互相覆盖。多实例收敛：Redis tryEnter 窗口把同一
 * 提供方一个窗口内的成簇推送收敛为一次（RemoteAiToolRegistryImpl 范式），Redis 异常降级直收。
 */
@Service
public class SysNotifyCatalogService {

    private static final Logger log = LoggerFactory.getLogger(SysNotifyCatalogService.class);

    /** 本地 Bean 提供组标识（单体聚合直收，不经 DB） */
    public static final String PROVIDER_LOCAL = "local";

    /** 同提供方推送收敛窗口：覆盖多实例成簇推送的时间差 */
    private static final Duration RECV_WINDOW = Duration.ofSeconds(30);

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final SysNotifyChannelMapper channelMapper;
    private final SysNotifySourceMapper sourceMapper;
    private final ObjectProvider<RemoteNotifyChannel> localChannels;
    private final ObjectProvider<NotifySourceProvider> localSourceProviders;
    private final ObjectProvider<LockOps> lockOps;

    public SysNotifyCatalogService(SysNotifyChannelMapper channelMapper,
                                   SysNotifySourceMapper sourceMapper,
                                   ObjectProvider<RemoteNotifyChannel> localChannels,
                                   ObjectProvider<NotifySourceProvider> localSourceProviders,
                                   ObjectProvider<LockOps> lockOps) {
        this.channelMapper = channelMapper;
        this.sourceMapper = sourceMapper;
        this.localChannels = localChannels;
        this.localSourceProviders = localSourceProviders;
        this.lockOps = lockOps;
    }

    /** 目录条目（本地 Bean 优先于 DB 登记行；同码时本地胜出） */
    public record CatalogEntry(String channelCode, String channelName, String description,
                               boolean authRequired, String entitlementCode, boolean receiverRequired,
                               String providerGroup, boolean local) {
    }

    /** 来源目录条目（本地 Bean 优先于 DB 登记行；同码时本地胜出） */
    public record SourceEntry(String bizCode, String bizName, List<NotifyTypeDescriptor> types,
                              String providerGroup, boolean local) {
    }

    /** 合并渠道目录：容器内 RemoteNotifyChannel Bean + 登记源 DB 行（tenant_id=0，runIgnore 全局视图） */
    public List<CatalogEntry> catalog() {
        Map<String, CatalogEntry> merged = new LinkedHashMap<>();
        for (RemoteNotifyChannel channel : localChannels) {
            for (NotifyChannelDescriptor descriptor : safeDescribe(channel)) {
                if (descriptor == null || isBlank(descriptor.getChannelCode())) {
                    continue;
                }
                String code = descriptor.getChannelCode().trim();
                merged.putIfAbsent(code, new CatalogEntry(code, descriptor.getChannelName(),
                        descriptor.getDescription(), descriptor.isAuthRequired(),
                        descriptor.getEntitlementCode(), descriptor.isReceiverRequired(),
                        PROVIDER_LOCAL, true));
            }
        }
        for (SysNotifyChannel row : TenantContext.runIgnore(() -> channelMapper.selectList(
                new LambdaQueryWrapper<SysNotifyChannel>()
                        .eq(SysNotifyChannel::getTenantId, MseConstants.PLATFORM_TENANT_ID)))) {
            if (isBlank(row.getChannelCode()) || merged.containsKey(row.getChannelCode())) {
                continue;
            }
            merged.put(row.getChannelCode(), new CatalogEntry(row.getChannelCode(), row.getChannelName(),
                    row.getDescription(), Boolean.TRUE.equals(row.getAuthRequired()),
                    row.getEntitlementCode(), !Boolean.FALSE.equals(row.getReceiverRequired()),
                    row.getProviderGroup(), false));
        }
        return List.copyOf(merged.values());
    }

    public Optional<CatalogEntry> find(String channelCode) {
        if (isBlank(channelCode)) {
            return Optional.empty();
        }
        return catalog().stream()
                .filter(entry -> entry.channelCode().equals(channelCode.trim()))
                .findFirst();
    }

    /** 合并来源目录：容器内 NotifySourceProvider Bean + 登记源 DB 行（tenant_id=0，runIgnore 全局视图） */
    public List<SourceEntry> sources() {
        Map<String, SourceEntry> merged = new LinkedHashMap<>();
        for (NotifySourceProvider provider : localSourceProviders) {
            for (NotifySourceDescriptor descriptor : safeDescribeSources(provider)) {
                if (descriptor == null || isBlank(descriptor.getBizCode())) {
                    continue;
                }
                String code = descriptor.getBizCode().trim();
                merged.putIfAbsent(code, new SourceEntry(code, descriptor.getBizName(),
                        descriptor.getTypes() == null ? List.of() : descriptor.getTypes(),
                        PROVIDER_LOCAL, true));
            }
        }
        for (SysNotifySource row : TenantContext.runIgnore(() -> sourceMapper.selectList(
                new LambdaQueryWrapper<SysNotifySource>()
                        .eq(SysNotifySource::getTenantId, MseConstants.PLATFORM_TENANT_ID)))) {
            if (isBlank(row.getBizCode()) || merged.containsKey(row.getBizCode())) {
                continue;
            }
            merged.put(row.getBizCode(), new SourceEntry(row.getBizCode(), row.getBizName(),
                    parseTypes(row.getTypes()), row.getProviderGroup(), false));
        }
        return List.copyOf(merged.values());
    }

    /** 提供方推送渠道登记（幂等 upsert；详见类注释） */
    public void register(String providerGroup, List<NotifyChannelDescriptor> descriptors) {
        if (descriptors == null || descriptors.isEmpty()) {
            return;
        }
        String group = normalizeGroup(providerGroup);
        if (!tryEnterWindow("mse:notify:channel-recv:" + group)) {
            return;
        }
        int changed = 0;
        for (NotifyChannelDescriptor descriptor : descriptors) {
            try {
                if (upsertChannel(descriptor, group)) {
                    changed++;
                }
            } catch (Exception e) {
                log.warn("[notify] 渠道登记失败，跳过: {} - {}",
                        descriptor == null ? null : descriptor.getChannelCode(), e.getMessage());
            }
        }
        if (changed > 0) {
            log.info("[notify] 渠道登记完成: provider={}, 推送 {} 条（新增/更新 {}）",
                    group, descriptors.size(), changed);
        }
    }

    /** 提供方推送来源登记（幂等 upsert；详见类注释） */
    public void registerSources(String providerGroup, List<NotifySourceDescriptor> sources) {
        List<NotifySourceDescriptor> descriptors = sources == null ? List.of() : sources;
        if (descriptors.isEmpty()) {
            return;
        }
        String group = normalizeGroup(providerGroup);
        if (!tryEnterWindow("mse:notify:source-recv:" + group)) {
            return;
        }
        int changed = 0;
        for (NotifySourceDescriptor descriptor : descriptors) {
            try {
                if (upsertSource(descriptor, group)) {
                    changed++;
                }
            } catch (Exception e) {
                log.warn("[notify] 来源登记失败，跳过: {} - {}",
                        descriptor == null ? null : descriptor.getBizCode(), e.getMessage());
            }
        }
        if (changed > 0) {
            log.info("[notify] 来源登记完成: provider={}, 推送 {} 条（新增/更新 {}）",
                    group, descriptors.size(), changed);
        }
    }

    private boolean upsertChannel(NotifyChannelDescriptor descriptor, String group) {
        String code = descriptor == null || descriptor.getChannelCode() == null
                ? null : descriptor.getChannelCode().trim();
        if (isBlank(code)) {
            log.warn("[notify] 渠道登记缺少 channelCode，跳过（provider={}）", group);
            return false;
        }
        if (descriptor.isAuthRequired() && isBlank(descriptor.getEntitlementCode())) {
            log.warn("[notify] 渠道 {} 声明需授权但缺权益编码，租户侧将始终不可用（provider={}）", code, group);
        }
        SysNotifyChannel existing = findByCode(code);
        if (existing == null) {
            try {
                TenantContext.runIgnore(() -> {
                    SysNotifyChannel row = new SysNotifyChannel();
                    row.setChannelCode(code);
                    row.setChannelName(descriptor.getChannelName());
                    row.setDescription(descriptor.getDescription());
                    row.setAuthRequired(descriptor.isAuthRequired());
                    row.setEntitlementCode(descriptor.getEntitlementCode());
                    row.setReceiverRequired(descriptor.getReceiverRequired());
                    row.setProviderGroup(group);
                    row.setLastPushTime(LocalDateTime.now());
                    channelMapper.insert(row);
                    return null;
                });
                log.info("[notify] 渠道登记新增: {}（provider={}）", code, group);
                return true;
            } catch (DataIntegrityViolationException e) {
                existing = findByCode(code);
                if (existing == null) {
                    throw e;
                }
                log.debug("[notify] 渠道登记并发落败，转同步既有行: {}", code);
            }
        }
        if (!group.equals(existing.getProviderGroup())) {
            log.warn("[notify] 渠道登记跳过: {} 已由 {} 登记（本次推送 {}），防覆盖",
                    code, existing.getProviderGroup(), group);
            return false;
        }
        boolean dirty = !Objects.equals(existing.getChannelName(), descriptor.getChannelName())
                || !Objects.equals(existing.getDescription(), descriptor.getDescription())
                || !Objects.equals(Boolean.TRUE.equals(existing.getAuthRequired()), descriptor.isAuthRequired())
                || !Objects.equals(existing.getEntitlementCode(), descriptor.getEntitlementCode())
                || !Objects.equals(existing.getReceiverRequired(), descriptor.getReceiverRequired());
        if (!dirty) {
            return false;
        }
        SysNotifyChannel target = existing;
        TenantContext.runIgnore(() -> {
            target.setChannelName(descriptor.getChannelName());
            target.setDescription(descriptor.getDescription());
            target.setAuthRequired(descriptor.isAuthRequired());
            target.setEntitlementCode(descriptor.getEntitlementCode());
            target.setReceiverRequired(descriptor.getReceiverRequired());
            target.setLastPushTime(LocalDateTime.now());
            channelMapper.updateById(target);
            return null;
        });
        return true;
    }

    private boolean upsertSource(NotifySourceDescriptor descriptor, String group) {
        String code = descriptor == null || descriptor.getBizCode() == null
                ? null : descriptor.getBizCode().trim();
        if (isBlank(code)) {
            log.warn("[notify] 来源登记缺少 bizCode，跳过（provider={}）", group);
            return false;
        }
        String typesJson = writeTypes(descriptor.getTypes());
        SysNotifySource existing = TenantContext.runIgnore(() -> sourceMapper.selectOne(
                new LambdaQueryWrapper<SysNotifySource>()
                        .eq(SysNotifySource::getBizCode, code)
                        .eq(SysNotifySource::getTenantId, MseConstants.PLATFORM_TENANT_ID)
                        .last("LIMIT 1")));
        if (existing == null) {
            try {
                TenantContext.runIgnore(() -> {
                    SysNotifySource row = new SysNotifySource();
                    row.setBizCode(code);
                    row.setBizName(descriptor.getBizName());
                    row.setTypes(typesJson);
                    row.setProviderGroup(group);
                    row.setLastPushTime(LocalDateTime.now());
                    sourceMapper.insert(row);
                    return null;
                });
                log.info("[notify] 来源登记新增: {}（provider={}）", code, group);
                return true;
            } catch (DataIntegrityViolationException e) {
                existing = TenantContext.runIgnore(() -> sourceMapper.selectOne(
                        new LambdaQueryWrapper<SysNotifySource>()
                                .eq(SysNotifySource::getBizCode, code)
                                .eq(SysNotifySource::getTenantId, MseConstants.PLATFORM_TENANT_ID)
                                .last("LIMIT 1")));
                if (existing == null) {
                    throw e;
                }
                log.debug("[notify] 来源登记并发落败，转同步既有行: {}", code);
            }
        }
        if (!group.equals(existing.getProviderGroup())) {
            log.warn("[notify] 来源登记跳过: {} 已由 {} 登记（本次推送 {}），防覆盖",
                    code, existing.getProviderGroup(), group);
            return false;
        }
        boolean dirty = !Objects.equals(existing.getBizName(), descriptor.getBizName())
                || !Objects.equals(existing.getTypes(), typesJson);
        if (!dirty) {
            return false;
        }
        SysNotifySource target = existing;
        TenantContext.runIgnore(() -> {
            target.setBizName(descriptor.getBizName());
            target.setTypes(typesJson);
            target.setLastPushTime(LocalDateTime.now());
            sourceMapper.updateById(target);
            return null;
        });
        return true;
    }

    private SysNotifyChannel findByCode(String channelCode) {
        return TenantContext.runIgnore(() -> channelMapper.selectOne(new LambdaQueryWrapper<SysNotifyChannel>()
                .eq(SysNotifyChannel::getChannelCode, channelCode)
                .eq(SysNotifyChannel::getTenantId, MseConstants.PLATFORM_TENANT_ID)
                .last("LIMIT 1")));
    }

    /** 登记收敛窗口（窗口内同提供方已处理则跳过；Redis 异常降级直收） */
    private boolean tryEnterWindow(String key) {
        LockOps ops = lockOps.getIfAvailable();
        if (ops == null) {
            return true;
        }
        try {
            return ops.tryEnter(key, RECV_WINDOW);
        } catch (Exception e) {
            log.debug("[notify] 登记收敛标记不可用，降级直收（{}）: {}", key, e.getMessage());
            return true;
        }
    }

    private List<NotifyTypeDescriptor> parseTypes(String json) {
        if (isBlank(json)) {
            return List.of();
        }
        try {
            List<NotifyTypeDescriptor> types = JSON.readValue(json,
                    JSON.getTypeFactory().constructCollectionType(List.class, NotifyTypeDescriptor.class));
            return types == null ? List.of() : new ArrayList<>(types);
        } catch (Exception e) {
            log.warn("[notify] 来源类型清单解析失败，按空处理: {}", e.getMessage());
            return List.of();
        }
    }

    private String writeTypes(List<NotifyTypeDescriptor> types) {
        if (types == null || types.isEmpty()) {
            return null;
        }
        try {
            return JSON.writeValueAsString(types);
        } catch (Exception e) {
            log.warn("[notify] 来源类型清单序列化失败，按空处理: {}", e.getMessage());
            return null;
        }
    }

    private List<NotifySourceDescriptor> safeDescribeSources(NotifySourceProvider provider) {
        try {
            List<NotifySourceDescriptor> descriptors = provider.describeSources();
            return descriptors == null ? List.of() : descriptors;
        } catch (Exception e) {
            log.warn("[notify] 收集本地来源描述失败，跳过: {} - {}", provider.getClass().getName(), e.getMessage());
            return List.of();
        }
    }

    private List<NotifyChannelDescriptor> safeDescribe(RemoteNotifyChannel channel) {
        try {
            List<NotifyChannelDescriptor> descriptors = channel.describeChannels();
            return descriptors == null ? List.of() : descriptors;
        } catch (Exception e) {
            log.warn("[notify] 收集本地渠道描述失败，跳过: {} - {}", channel.getClass().getName(), e.getMessage());
            return List.of();
        }
    }

    private static String normalizeGroup(String providerGroup) {
        return isBlank(providerGroup) ? "unknown" : providerGroup.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
