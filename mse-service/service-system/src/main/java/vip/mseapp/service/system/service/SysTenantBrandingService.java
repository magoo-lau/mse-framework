package vip.mseapp.service.system.service;

import com.baomidou.mybatisplus.extension.repository.IRepository;
import vip.mseapp.service.system.entity.SysTenantBranding;

/**
 * 租户品牌个性化（租户世界数据，每租户一行）：租户读写的都是本世界自己的行。
 */
public interface SysTenantBrandingService extends IRepository<SysTenantBranding> {

    /** 当前租户的品牌配置；未配置过返回 null */
    SysTenantBranding getOwn();

    /** 保存当前租户的品牌配置（未配置过则新建，已有则整体覆盖，空白字段清空） */
    void saveOwn(SysTenantBranding branding);

    /** 按租户读品牌配置（登录页解析链路；调用方须已 runAs 目标租户） */
    SysTenantBranding getByTenantId(Long tenantId);
}
