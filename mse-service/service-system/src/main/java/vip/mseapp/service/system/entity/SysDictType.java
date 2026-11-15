package vip.mseapp.service.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import vip.mseapp.mybatis.data.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典类型（租户隔离表，dict_type 租户内唯一）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_type")
public class SysDictType extends BaseEntity {

    /** 字典名称（如：用户性别） */
    private String dictName;

    /** 字典类型（如：sys_user_sex，业务按此取数据） */
    private String dictType;

    /** 0 正常 1 停用 */
    private Integer status;

    private String remark;
}
