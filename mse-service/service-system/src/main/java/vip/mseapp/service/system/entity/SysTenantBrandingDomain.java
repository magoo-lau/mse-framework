package vip.mseapp.service.system.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 租户品牌域名绑定（租户世界数据）：租户在品牌个性化页自助绑定的站点域名，
 * 访客经该域名打开登录页即呈现本租户品牌并免填租户编码。
 *
 * <p>不继承 {@link BaseEntity}：解绑是常规操作且域名全局唯一——物理删除配合
 * domain 唯一索引，避免逻辑删除行占用唯一键导致同名域名无法重新绑定。
 * tenant_id 仍由租户插件/MetaObjectHandler 按上下文自动填充。
 */
@Data
@TableName("sys_tenant_branding_domain")
public class SysTenantBrandingDomain implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键，雪花 ID（前端经 Jackson 序列化为字符串） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户编号（绑定归属；0=平台世界，本表正常只在租户世界产生行） */
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    /** 域名（完整主机名，小写，全局唯一——一个域名只归一个世界） */
    private String domain;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
