package vip.mseapp.service.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import vip.mseapp.api.tenant.dto.EntitlementDTO;
import vip.mseapp.mq.TenantEvent;
import vip.mseapp.mq.TenantEventSender;
import vip.mseapp.redis.MseEntCacheKeys;
import vip.mseapp.service.platform.entity.SysAddon;
import vip.mseapp.service.platform.entity.SysAddonItem;
import vip.mseapp.service.platform.entity.SysEntitlementItem;
import vip.mseapp.service.platform.entity.SysPackageItem;
import vip.mseapp.service.platform.entity.SysTenant;
import vip.mseapp.service.platform.entity.SysTenantAddon;
import vip.mseapp.service.platform.entity.SysTenantEntitlementOverride;
import vip.mseapp.service.platform.entity.SysTenantPackage;
import vip.mseapp.service.platform.mapper.SysAddonItemMapper;
import vip.mseapp.service.platform.mapper.SysAddonMapper;
import vip.mseapp.service.platform.mapper.SysEntitlementItemMapper;
import vip.mseapp.service.platform.mapper.SysPackageItemMapper;
import vip.mseapp.service.platform.mapper.SysTenantAddonMapper;
import vip.mseapp.service.platform.mapper.SysTenantMapper;
import vip.mseapp.service.platform.mapper.SysTenantEntitlementOverrideMapper;
import vip.mseapp.service.platform.service.EntitlementService;
import vip.mseapp.service.platform.service.SysTenantPackageService;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 权益计算（0.1.0 权益模型）：权益 = 套餐 menuIds（菜单域既有载体）+ 权益项链
 * {@code default → 套餐(sys_package_item) → 附加项(sys_addon_item×qty，配额类累加) →
 * 租户覆盖(override，绝对值最高优先)}。
 *
 * <p>类级 NOT_SUPPORTED 控制面堤坝（六张权益表只在平台库）。计算为控制面权威口径：
 * 快照（getTenantSnapshot）与契约（getTenantEntitlement）经本服务取值；数据面消费走
 * EntitlementCache（版本失效）或快照本地副本，运行期零调平台。
 */
@org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
public class EntitlementServiceImpl implements EntitlementService {

    private static final Logger log = LoggerFactory.getLogger(EntitlementServiceImpl.class);

    /** 配额类 item_type */
    private static final int TYPE_QUOTA = 1;

    private final SysEntitlementItemMapper itemMapper;

    private final SysPackageItemMapper packageItemMapper;

    private final SysAddonMapper addonMapper;

    private final SysAddonItemMapper addonItemMapper;

    private final SysTenantAddonMapper tenantAddonMapper;

    private final SysTenantEntitlementOverrideMapper overrideMapper;

    private final SysTenantPackageService packageService;

    private final SysTenantMapper tenantMapper;

    private final ObjectProvider<RedisTemplate<String, Object>> redisProvider;

    private final ObjectProvider<TenantEventSender> eventSenderProvider;

    public EntitlementServiceImpl(SysEntitlementItemMapper itemMapper,
                                  SysPackageItemMapper packageItemMapper,
                                  SysAddonMapper addonMapper,
                                  SysAddonItemMapper addonItemMapper,
                                  SysTenantAddonMapper tenantAddonMapper,
                                  SysTenantEntitlementOverrideMapper overrideMapper,
                                  SysTenantPackageService packageService,
                                  SysTenantMapper tenantMapper,
                                  ObjectProvider<RedisTemplate<String, Object>> redisProvider,
                                  ObjectProvider<TenantEventSender> eventSenderProvider) {
        this.itemMapper = itemMapper;
        this.packageItemMapper = packageItemMapper;
        this.addonMapper = addonMapper;
        this.addonItemMapper = addonItemMapper;
        this.tenantAddonMapper = tenantAddonMapper;
        this.overrideMapper = overrideMapper;
        this.packageService = packageService;
        this.tenantMapper = tenantMapper;
        this.redisProvider = redisProvider;
        this.eventSenderProvider = eventSenderProvider;
    }

    @Override
    public EntitlementDTO compute(Long tenantId) {
        EntitlementDTO dto = new EntitlementDTO();
        dto.setTenantId(tenantId);
        dto.setVersion(System.currentTimeMillis());

        // ① 权益项全集（启用的）→ 以 default 起底
        List<SysEntitlementItem> items = itemMapper.selectList(
                new LambdaQueryWrapper<SysEntitlementItem>()
                        .eq(SysEntitlementItem::getStatus, 0)
                        .orderByAsc(SysEntitlementItem::getId));
        Map<Long, SysEntitlementItem> itemById = new LinkedHashMap<>();
        Map<String, String> values = new LinkedHashMap<>();
        for (SysEntitlementItem item : items) {
            itemById.put(item.getId(), item);
            values.put(item.getItemCode(), item.getDefaultValue());
        }

        // ② 菜单域：租户 → 套餐 menuIds（未配套餐保持 null=宽松全量；
        // 绑定了套餐但套餐悬空/菜单串为空 = 空集，宁紧勿松——此前两分支均塌陷为 null 宽松全量）
        Long packageId = null;
        SysTenant tenant = tenantMapper.selectById(tenantId);
        if (tenant != null && tenant.getPackageId() != null) {
            packageId = tenant.getPackageId();
            SysTenantPackage pkg = packageService.getById(packageId);
            dto.setMenuIds(pkg == null ? List.of() : splitMenuIds(pkg.getMenuIds()));
        }

        // ③ 套餐覆盖
        if (packageId != null) {
            for (SysPackageItem pi : packageItemMapper.selectList(
                    new LambdaQueryWrapper<SysPackageItem>().eq(SysPackageItem::getPackageId, packageId))) {
                apply(values, itemById, pi.getItemId(), pi.getItemValue());
            }
        }

        // ④ 附加项（×qty：配额类按数量累加，开关类取值即生效）
        List<SysTenantAddon> subscriptions = tenantAddonMapper.selectList(
                new LambdaQueryWrapper<SysTenantAddon>()
                        .eq(SysTenantAddon::getTenantId, tenantId)
                        .eq(SysTenantAddon::getStatus, 0));
        for (SysTenantAddon subscription : subscriptions) {
            SysAddon addon = addonMapper.selectById(subscription.getAddonId());
            if (addon == null || Integer.valueOf(1).equals(addon.getStatus())) {
                continue;
            }
            int qty = subscription.getQty() == null || subscription.getQty() < 1 ? 1 : subscription.getQty();
            for (SysAddonItem ai : addonItemMapper.selectList(
                    new LambdaQueryWrapper<SysAddonItem>().eq(SysAddonItem::getAddonId, addon.getId()))) {
                SysEntitlementItem item = itemById.get(ai.getItemId());
                if (item == null) {
                    continue;
                }
                if (item.getItemType() != null && item.getItemType() == TYPE_QUOTA) {
                    long delta = parseLong(ai.getItemValue()) * qty;
                    values.merge(item.getItemCode(), String.valueOf(parseLong(values.get(item.getItemCode())) + delta),
                            (a, b) -> b);
                } else {
                    apply(values, itemById, ai.getItemId(), ai.getItemValue());
                }
            }
        }

        // ⑤ 租户覆盖（绝对值，最高优先）
        for (SysTenantEntitlementOverride ov : overrideMapper.selectList(
                new LambdaQueryWrapper<SysTenantEntitlementOverride>()
                        .eq(SysTenantEntitlementOverride::getTenantId, tenantId))) {
            apply(values, itemById, ov.getItemId(), ov.getItemValue());
        }

        // ⑥ 按类型分桶
        for (SysEntitlementItem item : items) {
            String value = values.get(item.getItemCode());
            if (value == null) {
                continue;
            }
            if (item.getItemType() != null && item.getItemType() == TYPE_QUOTA) {
                dto.getQuotas().put(item.getItemCode(), value);
            } else {
                dto.getFeatures().put(item.getItemCode(), value);
            }
        }
        return dto;
    }

    /** 覆盖指定权益项的值（item 缺席/停用时忽略——防御历史脏引用） */
    private void apply(Map<String, String> values, Map<Long, SysEntitlementItem> itemById, Long itemId, String value) {
        SysEntitlementItem item = itemById.get(itemId);
        if (item != null && value != null) {
            values.put(item.getItemCode(), value);
        }
    }

    private static long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private static List<Long> splitMenuIds(String menuIds) {
        // 空串 = 套餐显式未勾选任何菜单 = 空集（区别于 null=未配套餐宽松全量，契约见 RemoteTenantService）
        if (menuIds == null || menuIds.isBlank()) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        for (String part : menuIds.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                ids.add(Long.parseLong(trimmed));
            }
        }
        return ids;
    }

    // ===== 失效通知（写点调用：INCR 版本 + 事件） =====

    @Override
    public void evictGlobal() {
        increment(MseEntCacheKeys.globalVer());
        publishEvent(null, TenantEvent.Type.FEATURE_CHANGED, "entitlement-global-changed");
    }

    @Override
    public void evictTenant(Long tenantId) {
        increment(MseEntCacheKeys.tenantVer(tenantId));
        publishEvent(tenantId, TenantEvent.Type.FEATURE_CHANGED, "entitlement-tenant-changed");
    }

    private void increment(String key) {
        RedisTemplate<String, Object> redis = redisProvider.getIfAvailable();
        if (redis == null) {
            return;
        }
        try {
            redis.opsForValue().increment(key);
        } catch (Exception e) {
            log.warn("[mse-ent] 权益缓存版本递增失败（留待 TTL 兜底）: {}", e.getMessage());
        }
    }

    private void publishEvent(Long tenantId, String type, String payload) {
        TenantEventSender sender = eventSenderProvider.getIfAvailable();
        if (sender == null) {
            return;
        }
        try {
            sender.sendTenant(TenantEvent.TOPIC, tenantId, type, payload);
        } catch (Exception e) {
            log.debug("[mse-ent] 权益变更事件发送失败（快照/缓存 TTL 兜底）: {}", e.getMessage());
        }
    }

    /** resync 对账用：原始缓存读取（无加载） */
    @Override
    @SuppressWarnings("unchecked")
    public EntitlementDTO peekCached(Long tenantId) {
        RedisTemplate<String, Object> redis = redisProvider.getIfAvailable();
        if (redis == null) {
            return null;
        }
        try {
            long globalVer = readVersion(redis, MseEntCacheKeys.globalVer());
            long tenantVer = readVersion(redis, MseEntCacheKeys.tenantVer(tenantId));
            Object cached = redis.opsForValue().get(MseEntCacheKeys.entitlement(tenantId, globalVer, tenantVer));
            return cached instanceof EntitlementDTO dto ? dto : null;
        } catch (Exception e) {
            return null;
        }
    }

    private long readVersion(RedisTemplate<String, Object> redis, String key) {
        Object value = redis.opsForValue().get(key);
        return value == null ? 0L : Long.parseLong(value.toString());
    }

    /** resync 对账用：全部租户编号 */
    @Override
    public List<Long> allTenantIds() {
        return tenantMapper.selectList(new LambdaQueryWrapper<>()).stream()
                .map(SysTenant::getId).toList();
    }

    /** resync 对账用：缓存版本读取（对账漂移判定） */
    @Override
    public Map<String, Long> cacheVersions(Long tenantId) {
        RedisTemplate<String, Object> redis = redisProvider.getIfAvailable();
        Map<String, Long> versions = new HashMap<>();
        if (redis == null) {
            return versions;
        }
        versions.put("global", readVersion(redis, MseEntCacheKeys.globalVer()));
        versions.put("tenant", readVersion(redis, MseEntCacheKeys.tenantVer(tenantId)));
        return versions;
    }
}
