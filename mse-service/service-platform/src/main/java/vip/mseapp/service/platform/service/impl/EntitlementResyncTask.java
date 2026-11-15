package vip.mseapp.service.platform.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vip.mseapp.api.tenant.dto.EntitlementDTO;
import vip.mseapp.core.schedule.MseScheduled;
import vip.mseapp.service.platform.service.EntitlementService;

/**
 * 权益对账任务（0.1.0 调谐，mse.entitlement.resync-enabled 默认关）：周期重算全量租户权益，
 * 与 EntitlementCache 原始缓存比对漂移并 INFO 记录（首期仅日志对账，不自动回写——漂移多源于
 * 失效事件丢失，由 TTL 30min 兜底收敛；开启后可据日志决定是否需要补偿失效）。
 */
public class EntitlementResyncTask {

    private static final Logger log = LoggerFactory.getLogger(EntitlementResyncTask.class);

    private final EntitlementService entitlementService;

    public EntitlementResyncTask(EntitlementService entitlementService) {
        this.entitlementService = entitlementService;
    }

    @MseScheduled(taskId = "entitlement-resync", name = "权益对账",
            description = "重算全量租户权益与缓存对账（仅日志，默认关；mse.entitlement.resync-enabled）",
            fixedDelayString = "${mse.entitlement.resync-interval-ms:300000}",
            category = "platform")
    public void resync() {
        int drift = 0;
        int total = 0;
        for (Long tenantId : entitlementService.allTenantIds()) {
            total++;
            try {
                EntitlementDTO fresh = entitlementService.compute(tenantId);
                EntitlementDTO cached = entitlementService.peekCached(tenantId);
                if (cached != null && !fresh.getQuotas().equals(cached.getQuotas())) {
                    drift++;
                    log.warn("[mse-ent-resync] 权益缓存漂移: tenant={} cachedVersion={}（TTL 兜底收敛，"
                            + "必要时可对该租户触发覆盖保存补偿失效）", tenantId, cached.getVersion());
                }
            } catch (Exception e) {
                log.warn("[mse-ent-resync] 租户权益重算失败: tenant={} {}", tenantId, e.getMessage());
            }
        }
        if (total > 0) {
            log.info("[mse-ent-resync] 权益对账完成: 租户 {} 个、缓存漂移 {} 个", total, drift);
        }
    }
}
