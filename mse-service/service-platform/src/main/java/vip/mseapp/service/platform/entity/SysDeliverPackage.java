package vip.mseapp.service.platform.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import vip.mseapp.mybatis.data.PlatformBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 交付附件签发记录（平台库，审计台账）：一次签发 = license 凭证 + 租户数据 JSONL +
 * 租户变量 + 表结构（attachments format:1 附件包，RS256 签名内层）归档；
 * 最终交付包由 deliver-studio「套件 + 附件」重打包产出（平台不再装配套件）。
 * manifest 快照为内层 deploy-package.zip 的 manifest（逐文件校验与版本基线）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_deliver_package")
public class SysDeliverPackage extends PlatformBaseEntity {

    private Long tenantId;

    private String tenantCode;

    /** FULL 全量附件包 / UPGRADE 升级包 */
    private String packageType;

    /** 随包授权凭证编号 */
    private String licenseId;

    /** 签发模块集（逗号分隔；空=全量不过滤；驱动数据表按模块裁剪导出） */
    private String moduleList;

    /** 直推 deliver-studio 的附件记录号（未推送为空） */
    private String studioDeliveryId;

    /** 推送状态：pending 已排队 / pushed 已推送 / failed: 原因；空=未请求推送 */
    private String pushStatus;

    /** 1=共享库租户（按 tenant_id 过滤导出） */
    private Integer sharedMode;

    /** 任务状态：0 签发中 1 完成 2 失败 */
    private Integer status;

    /** 签发进度 0-100 */
    private Integer progress;

    /** 当前阶段描述 */
    private String stage;

    /** 失败原因 */
    private String errorMsg;

    private Integer tableCount;

    private Long rowCount;

    private String fileName;

    private String filePath;

    private Long fileSize;

    private String fileSha256;

    /** 内层 deploy-package.zip manifest 快照（含逐文件校验与版本基线） */
    private String manifestJson;

    private String remark;

    public static final String TYPE_FULL = "FULL";
    public static final String TYPE_UPGRADE = "UPGRADE";

    public static final int STATUS_RUNNING = 0;
    public static final int STATUS_DONE = 1;
    public static final int STATUS_FAILED = 2;
}
