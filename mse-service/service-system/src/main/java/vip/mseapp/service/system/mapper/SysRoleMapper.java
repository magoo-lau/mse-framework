package vip.mseapp.service.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import vip.mseapp.service.system.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /** 查询用户角色编码（登录/鉴权链路；租户条件由租户插件按上下文追加；ur 逻辑删除表必须过滤） */
    @Select("""
            SELECT DISTINCT r.code FROM sys_role r
            JOIN sys_user_role ur ON ur.role_id = r.id AND ur.deleted = 0
            WHERE ur.user_id = #{userId} AND r.status = 0 AND r.deleted = 0
            """)
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

    /** 查询用户全部启用角色的数据范围编号（多角色合并取最宽由调用方处理） */
    @Select("""
            SELECT DISTINCT r.data_scope FROM sys_role r
            JOIN sys_user_role ur ON ur.role_id = r.id AND ur.deleted = 0
            WHERE ur.user_id = #{userId} AND r.status = 0 AND r.deleted = 0
            """)
    List<Integer> selectDataScopesByUserId(@Param("userId") Long userId);

    /** 查询用户角色（自定义数据范围）勾选的部门集合（rd/ur 逻辑删除表必须过滤） */
    @Select("""
            SELECT DISTINCT rd.dept_id FROM sys_role_dept rd
            JOIN sys_user_role ur ON ur.role_id = rd.role_id AND ur.deleted = 0
            JOIN sys_role r ON r.id = rd.role_id
            WHERE ur.user_id = #{userId} AND r.status = 0 AND r.deleted = 0
              AND rd.deleted = 0 AND r.data_scope = 2
            """)
    List<Long> selectCustomDeptIdsByUserId(@Param("userId") Long userId);
}
