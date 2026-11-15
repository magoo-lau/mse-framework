package vip.mseapp.service.system.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志（租户隔离表，仅插入不更新——不继承 BaseEntity，表无 update_by/update_time 列）。
 */
@Data
@TableName("sys_oper_log")
public class SysOperLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 租户编号：0 表示平台级数据 */
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    /** 业务模块（如 system:user） */
    private String module;

    /** 操作类型（新增/修改/删除/导入/导出等） */
    private String operType;

    /** 方法名（类.方法） */
    private String method;

    /** 请求方式（GET/POST/PUT/DELETE） */
    private String requestMethod;

    /** 请求 URL */
    private String operUrl;

    /** 请求参数（JSON，超长截断） */
    private String operParam;

    private Long operUserId;

    private String operUsername;

    private String operIp;

    /** 耗时（毫秒） */
    private Integer costMs;

    /** 0 成功 1 失败 */
    private Integer status;

    /** 错误消息（超长截断） */
    private String errorMsg;

    private LocalDateTime operTime;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
