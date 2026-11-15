package vip.mseapp.service.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import vip.mseapp.mybatis.data.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典数据（租户隔离表，按 dict_type 归组）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_data")
public class SysDictData extends BaseEntity {

    /** 所属字典类型 */
    private String dictType;

    /** 数据标签（展示用） */
    private String dictLabel;

    /** 数据键值（存储用） */
    private String dictValue;

    /** 显示顺序 */
    private Integer dictSort;

    /** 标签样式（default/primary/success/info/warning/danger） */
    private String listClass;

    /** 是否默认 0 否 1 是 */
    private Integer isDefault;

    /** 0 正常 1 停用 */
    private Integer status;

    private String remark;
}
