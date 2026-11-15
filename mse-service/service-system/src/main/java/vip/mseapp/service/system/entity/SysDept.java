package vip.mseapp.service.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import vip.mseapp.mybatis.data.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 部门（租户隔离表，树形）。
 *
 * <p>ancestors 记录祖级编号链（如 "0,1000"），「本部门及以下」数据范围据此一次查询展开，
 * 避免递归下钻；调整父子关系时需级联刷新子树 ancestors。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dept")
public class SysDept extends BaseEntity {

    private Long parentId;

    /** 祖级列表（逗号分隔，根部门为 "0"） */
    private String ancestors;

    private String name;

    private Integer sort;

    /** 0 启用 1 停用 */
    private Integer status;

    /** 部门负责人（sys_user.id，空=未设置；IM 部门群群主据此派生） */
    private Long leaderUserId;
}
