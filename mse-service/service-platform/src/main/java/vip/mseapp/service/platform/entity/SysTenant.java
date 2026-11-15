package vip.mseapp.service.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import vip.mseapp.mybatis.data.PlatformBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 租户（平台级全局表，主键即租户维度）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant")
public class SysTenant extends PlatformBaseEntity {

    /** 租户编码（全局唯一，登录页可用） */
    private String code;

    private String name;

    private Long packageId;

    /**
     * 绑定数据源 key（必绑，分库部署）：
     * 共享库 → 该库 tenant_id 行级隔离；独立库 → dynamic-datasource 按该 key 路由租户专属库；
     * null = 未完成绑定（待开通状态，登录/路由明确拒绝）。
     */
    private String dsKey;

    /** 绑定域名（完整主机名，小写，全局唯一）；null 表示未绑定 */
    private String domain;

    private String contactUserName;

    private String contactPhone;

    /** 0 启用 1 停用 */
    private Integer status;

    /** 租户管理员账号（开通时登记，重试开通回显；密码明文不落库） */
    private String adminUsername;

    /** 管理员邮箱（开通时登记；重置密码/TOTP 的一次性凭证投递地址） */
    private String adminEmail;

    /** 开通状态：0 初始化中（后台开通任务执行中）1 已开通 2 开通失败；存量数据由 DDL 默认 1 兜底 */
    private Integer provisionStatus;

    /** 开通失败原因（provisionStatus=2 时有值） */
    private String provisionError;

    private LocalDateTime expireTime;

    private String remark;
}
