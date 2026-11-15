package vip.mseapp.service.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import vip.mseapp.mybatis.data.PlatformBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 租户×服务×表级隔离策略（平台级全局表，覆盖服务级默认）。
 *
 * <p>mode：SPLIT 按租户分表（order → order_t001）/ FIELD 字段隔离 / GLOBAL 全局共享不改写。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant_service_table_policy")
public class SysTenantServiceTablePolicy extends PlatformBaseEntity {

    private Long tenantId;

    private String serviceName;

    /** 逻辑表名（小写） */
    private String tableName;

    /** SPLIT / FIELD / GLOBAL（列名 table_mode，规避部分数据库保留字） */
    private String tableMode;

    /** 分表后缀（空 = 服务级 table_suffix） */
    private String tableSuffix;
}
