package vip.mseapp.service.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import vip.mseapp.service.system.entity.SysDomainRegistry;

/** 品牌域名全局注册表 Mapper（平台库；写入经 runIgnore 落 primary）。 */
@Mapper
public interface SysDomainRegistryMapper extends BaseMapper<SysDomainRegistry> {

    /** 按域名 + 属主摘除登记（解绑/补偿用；属主不符不删，防误摘易主域名） */
    @Delete("DELETE FROM sys_domain_registry WHERE domain = #{domain} AND tenant_id = #{tenantId}")
    int deleteByDomainAndTenant(@Param("domain") String domain, @Param("tenantId") Long tenantId);
}
