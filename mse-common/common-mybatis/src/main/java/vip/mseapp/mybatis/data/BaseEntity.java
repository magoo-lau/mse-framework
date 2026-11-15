package vip.mseapp.mybatis.data;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 租户业务表通用基类（表必须含 tenant_id 列）。
 *
 * <p>tenant_id 由 MetaObjectHandler 在插入时自动填充（显式赋值优先，不覆盖），租户改写由
 * TenantLineInnerInterceptor 完成；继承本基类即纳入租户行级隔离，业务代码禁止手工赋值
 * （平台跨租户运营写入除外，须 {@code @TenantIgnore} 并显式 setTenantId）。
 *
 * <p>tenant_id 0 的行 = 平台世界自有数据（平台端登录绑定租户 0，写入自动归 0）；
 * 两端完全独立，租户读不到平台行、平台读不到租户行，跨世界合并视图一律禁止——
 * 业务按「世界单边读写」口径访问本世界数据（runIgnore/runAs + 显式 tenant_id 条件）。
 */
@Data
public abstract class BaseEntity implements Serializable {

    /** 主键，雪花 ID（前端经 Jackson 序列化为字符串） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户编号：0 表示平台级数据 */
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除：0 未删除 1 已删除 */
    @TableLogic
    private Integer deleted;
}
