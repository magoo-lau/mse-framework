package vip.mseapp.core.license;

import java.util.List;
import java.util.Map;

/**
 * 授权凭证载荷（平台签发 → 租户侧验签消费）。
 *
 * <p>防伪模型：RS256 非对称签名——平台持私钥签发，租户侧只内置公钥验签，
 * 无法伪造/篡改 menuIds（越权防线：菜单下发、权限码、角色授权全部 ∩ 凭证 menuIds；
 * 本地 sys_menu 副本仅作展示元数据）。过期策略由消费方状态机决定（有效→宽限→锁定）。
 *
 * @param licenseId   凭证编号（平台侧签发记录主键，续期/审计用）
 * @param tenantCode  租户编码（租户侧与本机租户比对，防串用）
 * @param tenantName  租户名
 * @param packageName 套餐名
 * @param menuIds     套餐授权的菜单编号集合（权限域真源）
 * @param features    功能开关（预留，如 quota/feature 门控）
 * @param issuedAt    签发时间（epoch millis）
 * @param expiresAt   过期时间（epoch millis；租户侧到期后进入宽限期）
 */
public record LicensePayload(String licenseId, String tenantCode, String tenantName, String packageName,
                             List<Long> menuIds, Map<String, String> features,
                             long issuedAt, long expiresAt) {
}
