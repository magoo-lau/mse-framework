package vip.mseapp.service.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import vip.mseapp.mybatis.data.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 岗位（租户隔离表，post_code 租户内唯一）。
 *
 * <p>岗位仅用于给用户挂职务展示，不参与鉴权（权限只走 角色 → 权限点 链路）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_post")
public class SysPost extends BaseEntity {

    /** 岗位编码（如 ceo/se/hr） */
    private String postCode;

    /** 岗位名称 */
    private String postName;

    /** 显示顺序 */
    private Integer postSort;

    /** 0 正常 1 停用 */
    private Integer status;

    private String remark;
}
