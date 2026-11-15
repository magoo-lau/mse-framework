package vip.mseapp.service.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import vip.mseapp.mybatis.datascope.DataPermission;
import vip.mseapp.service.system.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 当前租户用户分页（数据权限示范查询）。
     *
     * <p>租户条件由租户插件追加；@DataPermission 使本查询按当前用户的数据范围
     * （部门/本人）追加行过滤——无用户上下文或范围为「全部」时原样执行。
     */
    @DataPermission
    @Select("""
            <script>
            SELECT * FROM sys_user
            WHERE deleted = 0
            <if test="username != null and username != ''">
              AND username LIKE CONCAT('%', #{username}, '%')
            </if>
            <if test="status != null">
              AND status = #{status}
            </if>
            <if test="deptId != null">
              AND (dept_id = #{deptId}
                   OR dept_id IN (SELECT id FROM sys_dept
                                  WHERE deleted = 0
                                    AND (ancestors = CONCAT('%,', #{deptId})
                                         OR ancestors LIKE CONCAT('%,', #{deptId}, ',%'))))
            </if>
            ORDER BY id DESC
            </script>
            """)
    Page<SysUser> selectPageUsers(Page<SysUser> page,
                                  @Param("username") String username,
                                  @Param("status") Integer status,
                                  @Param("deptId") Long deptId);

    /** 当前租户用户全量（导出用；数据范围与分页查询一致） */
    @DataPermission
    @Select("""
            <script>
            SELECT * FROM sys_user
            WHERE deleted = 0
            <if test="username != null and username != ''">
              AND username LIKE CONCAT('%', #{username}, '%')
            </if>
            <if test="status != null">
              AND status = #{status}
            </if>
            ORDER BY id DESC
            </script>
            """)
    List<SysUser> selectListUsers(@Param("username") String username,
                                  @Param("status") Integer status);
}
