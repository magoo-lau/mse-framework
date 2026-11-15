package vip.mseapp.service.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import vip.mseapp.mybatis.data.PlatformBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 租户数据源（平台级全局表，界面化维护的租户库）。
 *
 * <p>库模式：shared 共享库（多个租户绑定同一库，tenant_id 行级隔离）/
 * dedicated 独立库（单租户独占）。绑定关系经 {@code sys_tenant.ds_key} 引用本表 ds_key。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant_datasource")
public class SysTenantDatasource extends PlatformBaseEntity {

    public static final String MODE_SHARED = "shared";
    public static final String MODE_DEDICATED = "dedicated";

    /** 数据源 key（全局唯一；启动/变更时注册进 dynamic-datasource） */
    private String dsKey;

    /** 库模式：shared / dedicated */
    private String dsMode;

    /** 所属服务（spring.application.name，如 mse-oa；'*'=通用，所有服务可注册） */
    private String serviceName;

    /** 数据库类型：mysql / dm / kingbase */
    private String dbType;

    private String host;

    private Integer port;

    /** 库名（MySQL/金仓为 database；达梦为 schema） */
    private String dbName;

    /** 额外 JDBC 参数（追加到连接串） */
    private String params;

    private String username;

    /** 连接密码（AES 密文存储，密钥 mse.tenant.dynamic-ds.encrypt-key） */
    private String password;

    /** 0 启用 1 停用 */
    private Integer status;

    /** 初始化任务状态：NULL 未发起 0 初始化中 1 成功 2 失败（异步初始化落行，管理端轮询展示） */
    private Integer initStatus;

    /** 最近一次初始化结果摘要（成功统计/失败原因，截断 500 字符） */
    private String initMessage;

    /** 最近一次置「初始化中」的时间：启动对账恢复超时仍进行中的任务（进程重启丢任务自愈） */
    private java.time.LocalDateTime initStartedAt;

    /** 结构同步任务状态：NULL 未发起 0 同步中 1 成功 2 失败（单库与批量同步共用，落行展示） */
    private Integer syncStatus;

    /** 最近一次结构同步结果摘要（迁移+结构比对统计/失败原因，截断 500 字符） */
    private String syncMessage;

    /** 最近一次结构同步动作时间（发起/完成均刷新） */
    private java.time.LocalDateTime syncTime;

    /**
     * 缺省共享库标记（仅 shared 模式可标记，全局至多一个）：业务服务/单体的 master 即此库，
     * 创建租户界面默认选中。
     */

    private String remark;
}
