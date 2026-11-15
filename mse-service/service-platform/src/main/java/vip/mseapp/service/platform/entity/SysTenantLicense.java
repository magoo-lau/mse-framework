package vip.mseapp.service.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import vip.mseapp.mybatis.data.PlatformBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 授权凭证签发记录（平台库，审计台账；token 本体不落库，签发即交予运营导出）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant_license")
public class SysTenantLicense extends PlatformBaseEntity {

    /** 凭证编号（载荷内一致，租户侧续期/审计标识） */
    private String licenseId;

    private Long tenantId;

    /** 租户编码（载荷内一致，租户侧防串用） */
    private String tenantCode;

    private String packageName;

    /** 授权菜单编号（逗号分隔，审计用；运行域以载荷签名为准） */
    private String menuIds;

    private LocalDateTime issuedAt;

    private LocalDateTime expireAt;

    /** 0 有效 1 撤销 */
    private Integer status;

    public static final int STATUS_ACTIVE = 0;
    public static final int STATUS_REVOKED = 1;
}
