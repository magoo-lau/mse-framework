package vip.mseapp.service.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 品牌域名全局注册表行（平台库专用）：跨库「一个域名只归一个世界」的判重锚点。
 * 不继承 BaseEntity——本表只落平台库（runIgnore 路由 primary），无租户行级过滤与逻辑删除语义。
 */
@Data
@TableName("sys_domain_registry")
public class SysDomainRegistry implements Serializable {

    /** 来源：控制面 sys_tenant.domain */
    public static final int SOURCE_CONTROL_PLANE = 1;

    /** 来源：租户自助绑定 sys_tenant_branding_domain */
    public static final int SOURCE_SELF_SERVICE = 2;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 规范化域名（trim + 小写） */
    private String domain;

    /** 属主租户编号 */
    private Long tenantId;

    /** 来源：1=控制面 2=自助绑定 */
    private Integer source;

    private LocalDateTime createTime;
}
