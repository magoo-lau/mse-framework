package vip.mseapp.service.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import vip.mseapp.service.system.entity.SysDept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysDeptMapper extends BaseMapper<SysDept> {

    /**
     * 查询部门及其全部子孙部门编号（「本部门及以下」数据范围展开）。
     * 逗号定界匹配 ancestors 链中的任意祖级（0,1000 → ,0,1000, 含 ,1000,），
     * 单表一次展开无需递归；用 CONCAT+LIKE 而非 MySQL 专有的 FIND_IN_SET，三库通用。
     */
    @Select("""
            SELECT id FROM sys_dept
            WHERE (id = #{deptId}
                   OR CONCAT(',', ancestors, ',') LIKE CONCAT('%,', #{deptId}, ',%'))
              AND deleted = 0
            """)
    List<Long> selectDeptAndChildren(@Param("deptId") Long deptId);

    /** 查询直接与间接子部门（调整父子关系时级联刷新 ancestors 用） */
    @Select("""
            SELECT * FROM sys_dept
            WHERE CONCAT(',', ancestors, ',') LIKE CONCAT('%,', #{deptId}, ',%') AND deleted = 0
            """)
    List<SysDept> selectChildren(@Param("deptId") Long deptId);
}
