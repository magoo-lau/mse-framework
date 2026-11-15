package vip.mseapp.service.platform.service;

import com.baomidou.mybatisplus.extension.repository.IRepository;
import vip.mseapp.api.system.dto.UserDTO;
import vip.mseapp.core.result.PageResult;
import vip.mseapp.service.platform.entity.SysTenant;

import java.util.List;

public interface SysTenantService extends IRepository<SysTenant> {

    /**
     * 开通租户：创建租户行（标记初始化中）后立即返回，管理员账号创建与租户库首次克隆
     * 由 {@link TenantProvisioner} 后台执行，进度见 sys_tenant.provision_status
     */
    Long createTenant(SysTenant tenant, String adminUsername, String adminPassword);

    /** 重试开通（开通失败/中断的租户）：后台重跑开通任务；密码明文不落库，需重新录入 */
    void retryProvision(Long tenantId, String adminUsername, String adminPassword);

    PageResult<SysTenant> pageTenants(long pageNum, long pageSize, String name, Integer status);

    void updateTenant(SysTenant tenant);

    /** 启用/停用 */
    void changeStatus(Long tenantId, Integer status);

    /** 登录链路校验：存在性、状态、有效期 */
    SysTenant validateTenant(Long tenantId);

    /**
     * 重置租户管理员密码（一次性开通写入：按开通登记的管理员账号定位，平台不回读租户库；
     * 密码明文在此完成 BCrypt 散列后经契约下发）
     */
    void resetAdminPassword(Long tenantId, String username, String newPassword);

    /** 重置租户管理员 TOTP 双因子（一次性开通写入，按开通登记账号定位；管理员丢失验证器时的恢复手段） */
    void resetAdminTotp(Long tenantId, String username);
}
