package vip.mseapp.service.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import vip.mseapp.mybatis.data.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户（租户隔离表，username 租户内唯一）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    private String username;

    /** 部门编号（数据权限锚点） */
    private Long deptId;

    /** BCrypt 散列（禁止序列化到任何 HTTP 响应；仅认证链路内部比对） */
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String password;

    private String nickname;

    private String mobile;

    private String email;

    private String avatar;

    /** 0 启用 1 停用 */
    private Integer status;

    /** 是否租户管理员（拥有租户内全部权限，权限校验返回 *） */
    private Boolean superAdmin;

    /** TOTP 双因子密钥（Base32；空=未绑定。禁止序列化到任何 HTTP 响应） */
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String totpSecret;
}
