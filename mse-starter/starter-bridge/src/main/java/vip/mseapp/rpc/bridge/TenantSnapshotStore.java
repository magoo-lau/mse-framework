package vip.mseapp.rpc.bridge;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vip.mseapp.api.tenant.RemoteTenantService;
import vip.mseapp.api.tenant.dto.TenantRouteDTO;
import vip.mseapp.api.tenant.dto.TenantSnapshotDTO;
import vip.mseapp.core.annotation.RpcReference;
import vip.mseapp.core.concurrent.TtlBoundedCache;
import vip.mseapp.core.crypto.AesCipher;
import vip.mseapp.core.datasource.TablePolicy;
import vip.mseapp.core.datasource.TenantIsolationMode;
import vip.mseapp.core.datasource.TenantRoute;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 租户快照本地存储（数据面）：控制面下发的租户元数据/逐服务路由副本。
 *
 * <p>安全语义：平台侧 JSON → AES-GCM 加密 → HMAC-SHA256 签名；入缓存前验签，
 * 验签失败拒绝使用并告警（防篡改）。存储三级：内存 L1 → 后端 L2
 * （{@link SnapshotBackend}：在线=Redis 多实例共享 / 离线=加密文件热重载）→ 平台拉取自举
 * （拉取成功写穿 L2）。运行期路由解析零调平台——平台故障时内存与后端副本继续服务已开通租户。
 *
 * <p><b>时效保证（0.2.0 起，三级防线）</b>：内存 L1 不再是「事件到达才失效」的无期缓存——
 * ①事件失效（同 JVM Spring 事件 / MQ 监听，快路径）；②<b>版本号节流校验</b>（命中后每
 * {@code versionCheckIntervalMs} 读一次 Redis 租户版本号——与控制面失效 INCR 同源——不等即重建，
 * 无 MQ 部署下控制面改路由/表策略最迟本间隔收敛，秒级）；③L1 TTL 兜底（默认 60s，即便事件与
 * 版本校验双双失效，最终一致上限有界）。版本探测为稳定值但与快照持续不等时（如平台无 Redis、
 * 数据面有 Redis 的不对称部署）只触发一次重建不循环：仅当探测值<b>发生变化</b>才再次失效。
 */
public class TenantSnapshotStore {

    private static final Logger log = LoggerFactory.getLogger(TenantSnapshotStore.class);

    private final SnapshotBackend backend;

    private final byte[] hmacKey;

    private final javax.crypto.SecretKey aesKey;

    /** Jackson 2：AES+HMAC 载荷的写读同 mapper（自洽），版本由 Boot 4 的 jackson-2-bom 托管；
     *  与全站 Jackson 3 基线并存的知悉项——载荷只在本地进程加解密，跨 Jackson 语义无交互 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 进程内快照缓存（tenantId → 已解密路由视图）：TTL + 容量上限 + 周期清扫（见类注释时效三级防线） */
    private final TtlBoundedCache<Long, CachedSnapshot> cache;

    /** 快照版本号探测（tenantId → Redis 租户版本号；null=未装配跳过节流校验） */
    private final Function<Long, Long> versionProbe;

    private final long versionCheckIntervalMs;

    /** 节流校验游标：上次触发失效的探测值（防「稳定不匹配」死循环，见类注释） */
    private final Map<Long, Long> lastProbeVer = new ConcurrentHashMap<>();

    /** 首次加载回调（每租户本进程一次，租户级种子补偿的快路径入口；null=无） */
    private final Consumer<Long> firstLoadHook;

    /** 拉取通道（单体=本地直调 service-platform 实现；微服务=dubbo 消费 mse-platform） */
    @RpcReference
    private RemoteTenantService remoteTenantService;

    public TenantSnapshotStore(SnapshotBackend backend, String snapshotKey) {
        this(backend, snapshotKey, 60_000, 10_000, 5_000, null, null);
    }

    public TenantSnapshotStore(SnapshotBackend backend, String snapshotKey, long ttlMs, int maxEntries,
                               long versionCheckIntervalMs, Function<Long, Long> versionProbe,
                               Consumer<Long> firstLoadHook) {
        this.backend = backend;
        this.aesKey = AesCipher.parseKey(snapshotKey);
        this.hmacKey = sha256(snapshotKey);
        this.cache = new TtlBoundedCache<>(ttlMs, maxEntries, "mse-tenant-snapshot");
        this.versionCheckIntervalMs = versionCheckIntervalMs;
        this.versionProbe = versionProbe;
        this.firstLoadHook = firstLoadHook;
    }

    /** 失效缓存（租户事件到达时调用；下次读取按 后端→拉取 顺序重建） */
    public void invalidate(Long tenantId) {
        cache.remove(tenantId);
        lastProbeVer.remove(tenantId);
    }

    /** 全量失效（全局路由变更事件 tenantId=null 时） */
    public void invalidateAll() {
        cache.clear();
        lastProbeVer.clear();
    }

    /**
     * 取租户权益快照（0.1.0 权益模型：配额/开关，三级回源与路由同款）。
     * 平台不可用且后端无副本时返回 null（调用方快速失败或走契约缓存）。
     */
    public vip.mseapp.api.tenant.dto.EntitlementDTO entitlement(Long tenantId) {
        CachedSnapshot snapshot = snapshot(tenantId);
        return snapshot == null || snapshot.entitlement() == null
                ? new vip.mseapp.api.tenant.dto.EntitlementDTO() : snapshot.entitlement();
    }

    /**
     * 取租户的逐服务路由（本地自治；快照缺失/过期时自动向平台拉取自举一次）。
     * 平台不可用且后端无副本时返回 null（调用方快速失败，不静默落库）。
     */
    public Map<String, TenantRoute> routes(Long tenantId) {
        CachedSnapshot snapshot = snapshot(tenantId);
        return snapshot == null ? null : snapshot.routes();
    }

    /** 命中 L1（含版本节流校验）→ 后端 → 平台拉取；首次加载成功后触发回调（种子补偿快路径） */
    private CachedSnapshot snapshot(Long tenantId) {
        CachedSnapshot snapshot = cache.get(tenantId);
        if (snapshot == null) {
            snapshot = loadFromBackend(tenantId);
            if (snapshot == null) {
                snapshot = pullAndStore(tenantId);
            }
            if (snapshot != null) {
                cache.put(tenantId, snapshot);
                if (firstLoadHook != null) {
                    try {
                        firstLoadHook.accept(tenantId);
                    } catch (Exception e) {
                        log.debug("[mse-snapshot] 首次加载回调异常（忽略）: tenant={} {}", tenantId, e.getMessage());
                    }
                }
            }
            return snapshot;
        }
        return versionUnchecked(tenantId, snapshot);
    }

    /** 版本节流校验：探测值变化且与快照版本不符 → 失效走重建；未变化/一致/探测不可用 → 原样返回 */
    private CachedSnapshot versionUnchecked(Long tenantId, CachedSnapshot snapshot) {
        if (versionProbe == null || versionCheckIntervalMs <= 0) {
            return snapshot;
        }
        long now = System.currentTimeMillis();
        if (now - snapshot.versionCheckedAt() < versionCheckIntervalMs) {
            return snapshot;
        }
        Long probeVer;
        try {
            probeVer = versionProbe.apply(tenantId);
        } catch (Exception e) {
            log.debug("[mse-snapshot] 版本号探测失败（跳过本次校验，TTL 兜底）: tenant={} {}", tenantId, e.getMessage());
            return snapshot;
        }
        if (probeVer == null) {
            return snapshot;
        }
        if (probeVer.equals(snapshot.version())) {
            lastProbeVer.remove(tenantId);
            cache.put(tenantId, snapshot.withCheckedAt(now));
            return snapshot;
        }
        if (probeVer.equals(lastProbeVer.get(tenantId))) {
            // 探测值与快照持续不等但未变化（平台无 Redis 等不对称部署）：只重建一次，不循环
            cache.put(tenantId, snapshot.withCheckedAt(now));
            return snapshot;
        }
        log.info("[mse-snapshot] 租户版本号已变化，失效本地快照重建: tenant={} snapshotVer={} probeVer={}",
                tenantId, snapshot.version(), probeVer);
        lastProbeVer.put(tenantId, probeVer);
        invalidate(tenantId);
        CachedSnapshot reloaded = loadFromBackend(tenantId);
        if (reloaded == null) {
            reloaded = pullAndStore(tenantId);
        }
        if (reloaded != null) {
            cache.put(tenantId, reloaded);
        }
        return reloaded;
    }

    /** DTO → 权益快照（quotas/features 快照携带；旧快照缺字段时为空集，不阻断） */
    private static vip.mseapp.api.tenant.dto.EntitlementDTO toEntitlement(TenantSnapshotDTO dto) {
        vip.mseapp.api.tenant.dto.EntitlementDTO entitlement = new vip.mseapp.api.tenant.dto.EntitlementDTO();
        entitlement.setTenantId(dto.getTenantId());
        entitlement.setQuotas(dto.getQuotas() == null ? new java.util.LinkedHashMap<>() : dto.getQuotas());
        entitlement.setFeatures(dto.getFeatures() == null ? new java.util.LinkedHashMap<>() : dto.getFeatures());
        entitlement.setVersion(dto.getSnapshotVersion() == null ? 0L : dto.getSnapshotVersion());
        return entitlement;
    }

    /** DTO 路由 → 运行时 TenantRoute（serviceName 维度索引） */
    private static Map<String, TenantRoute> toRoutes(TenantSnapshotDTO dto) {
        Map<String, TenantRoute> routes = new LinkedHashMap<>();
        if (dto.getRoutes() == null) {
            return routes;
        }
        for (TenantRouteDTO route : dto.getRoutes()) {
            Map<String, TablePolicy> policies = new LinkedHashMap<>();
            if (route.getTablePolicies() != null) {
                route.getTablePolicies().forEach(policy -> policies.put(policy.getTableName(),
                        new TablePolicy(TablePolicy.Mode.valueOf(policy.getMode()), policy.getTableSuffix())));
            }
            routes.put(route.getServiceName() == null ? "*" : route.getServiceName(),
                    new TenantRoute(route.getTenantId(), route.getServiceName(),
                            TenantIsolationMode.valueOf(route.getMode()), route.getDsKey(),
                            route.getTableSuffix(), route.getTenantColumn(), policies));
        }
        return routes;
    }

    private CachedSnapshot loadFromBackend(Long tenantId) {
        if (backend == null) {
            return null;
        }
        try {
            String stored = backend.read(tenantId);
            if (stored == null) {
                return null;
            }
            String[] parts = stored.split("\n", -1);
            if (parts.length != 3) {
                log.warn("[mse-snapshot] 后端快照载荷格式非法，拒绝使用: tenant={}", tenantId);
                return null;
            }
            return decrypt(parts[0], parts[1], Long.parseLong(parts[2]), tenantId);
        } catch (Exception e) {
            log.debug("[mse-snapshot] 后端快照读取不可用: tenant={} {}", tenantId, e.getMessage());
            return null;
        }
    }

    /** 向平台拉取快照，验签后写穿后端（自举/刷新通道；平台不可用返回 null 不阻断） */
    private CachedSnapshot pullAndStore(Long tenantId) {
        TenantSnapshotDTO dto;
        try {
            dto = remoteTenantService.getTenantSnapshot(tenantId);
        } catch (Exception e) {
            log.debug("[mse-snapshot] 平台快照拉取不可用（平台服务故障/未就绪，本地自治继续）: tenant={} {}",
                    tenantId, e.getMessage());
            return null;
        }
        if (dto == null) {
            return null;
        }
        try {
            String json = objectMapper.writeValueAsString(dto);
            String payload = AesCipher.encrypt(json, aesKey);
            String signature = hmac(payload);
            long version = dto.getSnapshotVersion() == null ? System.currentTimeMillis() : dto.getSnapshotVersion();
            if (backend != null) {
                backend.write(tenantId, payload, signature, version);
            }
            log.info("[mse-snapshot] 租户快照已拉取并写穿后端: tenant={} version={}", tenantId, version);
            return new CachedSnapshot(toRoutes(dto), toEntitlement(dto), version, System.currentTimeMillis());
        } catch (Exception e) {
            log.warn("[mse-snapshot] 租户快照落后端失败（内存缓存仍生效）: tenant={} {}", tenantId, e.getMessage());
            return null;
        }
    }

    /** 验签 → 解密 → 反序列化；任一步失败=疑似篡改/密钥不一致，拒绝入缓存 */
    private CachedSnapshot decrypt(String payload, String signature, long version, Long tenantId) {
        try {
            if (!hmac(payload).equals(signature)) {
                log.warn("[mse-snapshot] 快照验签失败（疑似篡改或密钥不一致），拒绝使用: tenant={}", tenantId);
                return null;
            }
            TenantSnapshotDTO dto = objectMapper.readValue(AesCipher.decrypt(payload, aesKey), TenantSnapshotDTO.class);
            return new CachedSnapshot(toRoutes(dto), toEntitlement(dto), version, System.currentTimeMillis());
        } catch (Exception e) {
            log.warn("[mse-snapshot] 快照解密失败（密钥不一致？）: tenant={} {}", tenantId, e.getMessage());
            return null;
        }
    }

    private String hmac(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacKey, "HmacSHA256"));
            return java.util.HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC 计算失败", e);
        }
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    record CachedSnapshot(Map<String, TenantRoute> routes,
                          vip.mseapp.api.tenant.dto.EntitlementDTO entitlement,
                          long version, long versionCheckedAt) {

        CachedSnapshot withCheckedAt(long checkedAt) {
            return new CachedSnapshot(routes, entitlement, version, checkedAt);
        }
    }
}
