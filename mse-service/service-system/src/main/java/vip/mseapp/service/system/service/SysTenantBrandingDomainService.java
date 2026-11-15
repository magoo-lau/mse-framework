package vip.mseapp.service.system.service;

import com.baomidou.mybatisplus.extension.repository.IRepository;
import vip.mseapp.service.system.entity.SysTenantBrandingDomain;

import java.util.List;

/**
 * 租户品牌域名绑定（租户自助）：租户读写的都是本世界自己的行；域名全局唯一。
 */
public interface SysTenantBrandingDomainService extends IRepository<SysTenantBrandingDomain> {

    /** 当前租户的域名绑定列表（按绑定时间升序） */
    List<SysTenantBrandingDomain> listOwn();

    /** 当前租户绑定新域名（规范化 + 全局查重 + 落库 + 域名缓存写穿）；返回绑定行 */
    SysTenantBrandingDomain bindOwn(String domain);

    /** 当前租户解绑域名（物理删除 + 域名缓存摘除） */
    void unbindOwn(Long id);

    /** 按租户读绑定列表（缓存重建链路；调用方须已 runAs 目标租户） */
    List<SysTenantBrandingDomain> listByTenantId(Long tenantId);
}
