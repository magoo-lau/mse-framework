package vip.mseapp.service.system.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import vip.mseapp.service.system.service.TenantScopeService;
import vip.mseapp.service.system.notify.SysNotifyCatalogService.CatalogEntry;

/**
 * 通知渠道租户授权校验（框架层，对整租户生效——不论哪个业务发送）：authRequired=false 的
 * 渠道（站内信）直通；授权制渠道按基座权益模型的 {@code feature.*} 开关判定（平台管理员经
 * 「附加项/权益覆盖」按租户开通）。
 *
 * <p>读值复用 {@link TenantScopeService#getEntitlementValues}（本地 Bean + EntitlementCache
 * 版本缓存，变更秒级生效）。失败方向（QuotaGuard 范式）：读值异常 fail-open 放行（投递可用性
 * 优先，listChannels 仍只展示已授权渠道）；权益键缺失/值为 false = 明确未授权，fail-closed
 * 拦截；平台世界（tenant 0）放行。
 */
@Service
public class NotifyChannelAuthService {

    private static final Logger log = LoggerFactory.getLogger(NotifyChannelAuthService.class);

    private final TenantScopeService tenantScopeService;

    public NotifyChannelAuthService(TenantScopeService tenantScopeService) {
        this.tenantScopeService = tenantScopeService;
    }

    /**
     * 渠道在指定租户是否已授权。
     *
     * @param authRequired    渠道是否声明需授权（目录登记值）
     * @param entitlementCode 权益开关编码（authRequired=true 时应为 feature.* 项）
     * @param tenantId        租户编号（0/空 = 平台世界，放行）
     */
    public boolean authorized(boolean authRequired, String entitlementCode, Long tenantId) {
        if (!authRequired) {
            return true;
        }
        if (tenantId == null || tenantId == 0L) {
            return true;
        }
        if (entitlementCode == null || entitlementCode.isBlank()) {
            // 登记不完整（需授权但无权益编码）：按未授权拦截，防误放行
            return false;
        }
        try {
            String value = tenantScopeService.getEntitlementValues(tenantId).get(entitlementCode);
            return value != null && Boolean.parseBoolean(value.trim());
        } catch (Exception e) {
            log.warn("[notify] 租户权益读取失败，渠道授权校验 fail-open: {} - {}", entitlementCode, e.getMessage());
            return true;
        }
    }

    /** 目录条目便捷入口 */
    public boolean authorized(CatalogEntry entry, Long tenantId) {
        return authorized(entry.authRequired(), entry.entitlementCode(), tenantId);
    }
}
