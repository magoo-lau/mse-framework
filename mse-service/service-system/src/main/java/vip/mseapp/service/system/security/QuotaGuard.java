package vip.mseapp.service.system.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vip.mseapp.core.exception.ServiceException;
import vip.mseapp.service.system.service.TenantScopeService;

import java.util.Map;

/**
 * 配额守卫（0.1.0 权益模型·准入最小集）：创建用户前检查 {@code quota.max_users}
 * （软配额——并发窗口可容忍，不加重锁）。权益来源经 {@link TenantScopeService#getEntitlementValues}
 * （在线=EntitlementCache 回源契约；standalone=license features），控制面故障时缓存/TTL 继续服务。
 * 未配置该权益项或值非法 = 不设限（fail-open，与开关类默认语义一致）。
 */
public class QuotaGuard {

    private static final Logger log = LoggerFactory.getLogger(QuotaGuard.class);

    /** 用户数上限权益项编码 */
    public static final String ITEM_MAX_USERS = "quota.max_users";

    /** 配额超限错误码（service-system 12xx 段） */
    public static final int ERR_QUOTA_EXCEEDED = 1250;

    private final TenantScopeService tenantScopeService;

    public QuotaGuard(TenantScopeService tenantScopeService) {
        this.tenantScopeService = tenantScopeService;
    }

    /** 创建用户前校验：当前租户用户数已达上限时抛 1250 */
    public void checkMaxUsers(Long tenantId, long currentUsers) {
        if (tenantId == null || tenantId == 0L) {
            return; // 平台世界不设限（平台账号不受租户配额约束）
        }
        long limit = parseLimit(tenantScopeService.getEntitlementValues(tenantId).get(ITEM_MAX_USERS));
        if (limit <= 0) {
            return;
        }
        if (currentUsers >= limit) {
            log.warn("[mse-quota] 租户用户数达配额上限: tenant={} current={} limit={}", tenantId, currentUsers, limit);
            throw new ServiceException(ERR_QUOTA_EXCEEDED,
                    "用户数已达套餐配额上限（" + limit + "），请联系平台升级套餐或增购附加项");
        }
    }

    private long parseLimit(String value) {
        if (value == null || value.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
}
