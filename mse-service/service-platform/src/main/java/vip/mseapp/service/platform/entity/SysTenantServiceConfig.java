package vip.mseapp.service.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import vip.mseapp.mybatis.data.PlatformBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 租户×服务隔离配置（平台级全局表）。
 *
 * <p>isolation_mode：INDEPENDENT_DB 独立库（@TenantDatasource 实体路由 ds_key）/
 * SHARED_TABLE_SPLIT 共享库分表（按 table_suffix 改写表名）/
 * SHARED_FIELD 共享库字段隔离（现状默认行为）。
 * 无配置的租户×服务 = SHARED_FIELD；sys_tenant.ds_key 为全局默认兜底（仅独立库语义）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant_service_config")
public class SysTenantServiceConfig extends PlatformBaseEntity {

    public static final String MODE_INDEPENDENT = "INDEPENDENT_DB";
    public static final String MODE_SPLIT = "SHARED_TABLE_SPLIT";
    public static final String MODE_FIELD = "SHARED_FIELD";

    private Long tenantId;

    /** 服务名（spring.application.name，如 mse-oa） */
    private String serviceName;

    /** 隔离模式：INDEPENDENT_DB / SHARED_TABLE_SPLIT / SHARED_FIELD */
    private String isolationMode;

    /** 路由数据源 key；空 = 不路由（落本服务业务主库） */
    private String dsKey;

    /** 服务级分表后缀（SHARED_TABLE_SPLIT 缺省；分表 DDL 与运行时改写共用） */
    private String tableSuffix;

    /** 字段隔离列名（服务级；空 = tenant_id） */
    private String tenantColumn;

    private String remark;
}
