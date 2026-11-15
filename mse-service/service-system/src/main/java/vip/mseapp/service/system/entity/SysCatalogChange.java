package vip.mseapp.service.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import vip.mseapp.mybatis.data.PlatformBaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 定义目录变更批次（平台控制面表）：平台管理端对目录（sys_menu/sys_permission/sys_micro_app）
 * 的运行期编辑渲染为幂等 SQL 后落本表，由 {@code CatalogChangeDispatcher} 周期调谐扇出到
 * 租户库车队（登记库全集，账本 biz=catalog 按批次 id 幂等）。
 *
 * <p><b>append-only 纪律</b>：已扇出的批次不改 sql_text（账本 checksum 冲突纪律，与「已应用脚本
 * 不再修改」同理），写错了追加新批次纠正。status 仅监控用，正确性靠各库账本。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_catalog_change")
public class SysCatalogChange extends PlatformBaseEntity {

    /** 目标表：sys_menu */
    public static final String TABLE_MENU = "sys_menu";

    /** 目标表：sys_permission */
    public static final String TABLE_PERMISSION = "sys_permission";

    /** 目标表：sys_micro_app */
    public static final String TABLE_MICRO_APP = "sys_micro_app";

    /** 操作：upsert（存在则覆盖、不存在则插入） */
    public static final int OP_UPSERT = 0;

    /** 操作：逻辑删除 */
    public static final int OP_DELETE = 1;

    /** 状态：待扇出 */
    public static final int STATUS_PENDING = 0;

    /** 状态：已扇出（监控用） */
    public static final int STATUS_DISPATCHED = 1;

    /** 目标表名（sys_menu|sys_permission|sys_micro_app） */
    private String targetTable;

    /** 目标行主键 */
    private Long rowId;

    /** 0=upsert 1=逻辑删除 */
    private Integer op;

    /** 渲染好的幂等变更 SQL（ANSI，可多语句） */
    private String sqlText;

    /** 0=待扇出 1=已扇出 */
    private Integer status;
}
