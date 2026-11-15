package vip.mseapp.service.platform.service;

import com.baomidou.mybatisplus.extension.repository.IRepository;
import vip.mseapp.core.result.PageResult;
import vip.mseapp.service.platform.entity.SysDomainConfig;

/**
 * 域名个性化配置：按访问域名定制登录页标题 / Logo / 背景 / 版权（平台维护，全局表）。
 */
public interface SysDomainConfigService extends IRepository<SysDomainConfig> {

    /** 分页查询（平台管理页） */
    PageResult<SysDomainConfig> pageDomainConfigs(long pageNum, long pageSize, String domain, Integer status);

    /** 新增（域名规范化 + 唯一性校验），返回编号 */
    Long createDomainConfig(SysDomainConfig config);

    /** 更新（域名不可改；个性化字段支持清空） */
    void updateDomainConfig(SysDomainConfig config);

    /** 删除 */
    void deleteDomainConfig(Long id);

    /** 按域名取启用中的配置（登录页公开解析；未配置/停用返回 null） */
    SysDomainConfig getEnabledByDomain(String domain);
}
