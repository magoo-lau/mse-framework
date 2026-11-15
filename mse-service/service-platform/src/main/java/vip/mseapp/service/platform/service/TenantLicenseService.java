package vip.mseapp.service.platform.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import org.springframework.beans.factory.annotation.Value;
import vip.mseapp.core.exception.ServiceException;
import vip.mseapp.core.license.LicenseCodec;
import vip.mseapp.core.license.LicensePayload;
import vip.mseapp.service.platform.entity.SysTenant;
import vip.mseapp.service.platform.entity.SysTenantLicense;
import vip.mseapp.service.platform.entity.SysTenantPackage;
import vip.mseapp.service.platform.mapper.SysTenantLicenseMapper;
import vip.mseapp.service.platform.mapper.SysTenantMapper;
import vip.mseapp.service.platform.mapper.SysTenantPackageMapper;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 授权凭证签发（私有部署授权）：平台持 RSA 私钥（{@code mse.license.private-key}）签发
 * RS256 凭证，租户侧内置公钥验签——凭证 menuIds 即租户权限域真源，本地改表无法越权；
 * 到期 → 宽限（默认 15 天）→ 锁定的状态机在租户侧 {@code LicenseHolder} 执行。
 *
 * <p>签发记录落 {@code sys_tenant_license} 审计台账（token 本体不落库，仅签发时返回一次，
 * 运营导出交付租户）。
 */
public class TenantLicenseService extends CrudRepository<SysTenantLicenseMapper, SysTenantLicense> {

    private final SysTenantLicenseMapper licenseMapper;
    private final SysTenantMapper tenantMapper;
    private final SysTenantPackageMapper packageMapper;
    private final String privateKey;

    private final org.springframework.beans.factory.ObjectProvider<vip.mseapp.service.platform.service.EntitlementService> entitlementProvider;

    public TenantLicenseService(SysTenantLicenseMapper licenseMapper,
                                SysTenantMapper tenantMapper,
                                SysTenantPackageMapper packageMapper,
                                @Value("${mse.license.private-key:}") String privateKey,
                                org.springframework.beans.factory.ObjectProvider<vip.mseapp.service.platform.service.EntitlementService> entitlementProvider) {
        this.licenseMapper = licenseMapper;
        this.tenantMapper = tenantMapper;
        this.packageMapper = packageMapper;
        this.privateKey = privateKey;
        this.entitlementProvider = entitlementProvider;
    }

    /** 权益链 → license features（quota 与 feature 前缀项全量；缺席时空集） */
    private Map<String, String> licenseFeatures(Long tenantId) {
        vip.mseapp.service.platform.service.EntitlementService entitlement = entitlementProvider.getIfAvailable();
        if (entitlement == null) {
            return Map.of();
        }
        try {
            Map<String, String> features = new java.util.LinkedHashMap<>(entitlement.compute(tenantId).getQuotas());
            features.putAll(entitlement.compute(tenantId).getFeatures());
            return features;
        } catch (Exception e) {
            return Map.of();
        }
    }

    /** 签发并落审计台账；菜单域取租户套餐（兼容未提供勾选的旧入口） */
    public IssuedLicense issue(Long tenantId, int days) {
        return issue(tenantId, days, null);
    }

    /**
     * 签发并落审计台账（离线导出对话框显式勾选菜单）。
     *
     * @param menuIds 随包导出菜单（对话框树勾选，含半选父目录以保祖先链闭合）；
     *                null/空 = 回退租户套餐 menuIds。超界防御靠 standalone 侧
     *                license ∩ 本地租户域菜单兜底——勾选租户库不存在的菜单自然无效
     */
    public IssuedLicense issue(Long tenantId, int days, List<Long> menuIds) {
        SysTenant tenant = tenantMapper.selectById(tenantId);
        if (tenant == null) {
            throw new ServiceException(404, "租户不存在");
        }
        if (privateKey == null || privateKey.isBlank()) {
            throw new ServiceException(400, "平台未配置授权签发私钥（mse.license.private-key），无法签发");
        }
        if (days < 1 || days > 3650) {
            throw new ServiceException(400, "有效期须在 1~3650 天之间");
        }
        String packageName = null;
        List<Long> selected = menuIds == null ? List.of() : menuIds;
        if (tenant.getPackageId() != null) {
            SysTenantPackage pack = packageMapper.selectById(tenant.getPackageId());
            if (pack != null) {
                packageName = pack.getName();
                if (selected.isEmpty() && pack.getMenuIds() != null && !pack.getMenuIds().isBlank()) {
                    selected = Arrays.stream(pack.getMenuIds().split(","))
                            .map(String::trim).filter(s -> !s.isEmpty())
                            .map(Long::parseLong).toList();
                }
            }
        }
        List<Long> finalMenuIds = selected.stream().distinct().toList();
        if (finalMenuIds.isEmpty()) {
            throw new ServiceException(400, "随包导出菜单为空（请在导出对话框勾选，或为租户配置套餐）");
        }
        LocalDateTime now = LocalDateTime.now().withNano(0);
        LocalDateTime expire = now.plusDays(days);
        String licenseId = "LIC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        // 0.1.0 权益模型：license features 从权益链灌入（standalone 的 LicenseTenantScopeService
        // 即以 features 为离线权益快照——quota 与 feature 前缀项全量携带，闭环）
        Map<String, String> features = licenseFeatures(tenantId);
        LicensePayload payload = new LicensePayload(licenseId, tenant.getCode(), tenant.getName(),
                packageName, finalMenuIds, features,
                now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                expire.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        String token = LicenseCodec.issue(payload, privateKey);

        SysTenantLicense record = new SysTenantLicense();
        record.setLicenseId(licenseId);
        record.setTenantId(tenantId);
        record.setTenantCode(tenant.getCode());
        record.setPackageName(packageName);
        record.setMenuIds(packMenuIds(finalMenuIds));
        record.setIssuedAt(now);
        record.setExpireAt(expire);
        record.setStatus(SysTenantLicense.STATUS_ACTIVE);
        save(record);
        return new IssuedLicense(licenseId, token, expire.toString());
    }

    public List<SysTenantLicense> list(Long tenantId) {
        return list(new LambdaQueryWrapper<SysTenantLicense>()
                .eq(tenantId != null, SysTenantLicense::getTenantId, tenantId)
                .orderByDesc(SysTenantLicense::getId));
    }

    /** 撤销（台账标记；已交付凭证由租户侧有效期与续期控制，撤销用于审计与续期拒绝依据） */
    public void revoke(String licenseId) {
        SysTenantLicense record = getOne(new LambdaQueryWrapper<SysTenantLicense>()
                .eq(SysTenantLicense::getLicenseId, licenseId));
        if (record == null) {
            throw new ServiceException(404, "签发记录不存在");
        }
        SysTenantLicense patch = new SysTenantLicense();
        patch.setId(record.getId());
        patch.setStatus(SysTenantLicense.STATUS_REVOKED);
        updateById(patch);
    }

    private static String packMenuIds(List<Long> menuIds) {
        StringBuilder sb = new StringBuilder();
        for (Long id : menuIds) {
            if (!sb.isEmpty()) {
                sb.append(',');
            }
            sb.append(id);
        }
        return sb.toString();
    }

    /** 签发结果（token 仅本次返回，导出保存为 .license 文件交付租户） */
    public record IssuedLicense(String licenseId, String token, String expireAt) {
    }
}
