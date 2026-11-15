package vip.mseapp.service.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import vip.mseapp.service.system.entity.SysPermission;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

@Mapper
public interface SysPermissionMapper extends BaseMapper<SysPermission> {

    /** 查询用户权限标识集合（sys_permission 为全局表，关联表租户条件由插件追加；
     * ur/rp 为逻辑删除表，必须过滤 deleted——否则被回收的授权仍会联出，权限永不回收） */
    @Select("""
            SELECT DISTINCT p.code FROM sys_permission p
            JOIN sys_role_permission rp ON rp.permission_id = p.id AND rp.deleted = 0
            JOIN sys_user_role ur ON ur.role_id = rp.role_id AND ur.deleted = 0
            JOIN sys_role r ON r.id = ur.role_id
            WHERE ur.user_id = #{userId} AND p.status = 0 AND p.deleted = 0
              AND r.status = 0 AND r.deleted = 0
            """)
    List<String> selectCodesByUserId(@Param("userId") Long userId);

    /** 通用权限码（未挂菜单：自动授予全部登录用户，如个人资料） */
    @Select("""
            SELECT code FROM sys_permission
            WHERE menu_id IS NULL AND status = 0 AND deleted = 0
            """)
    List<String> selectUniversalCodes();

    /**
     * 查询菜单域内的权限点（含通用权限 menu_id IS NULL）。
     * menuIds 为 null 返回全部；空集合仅返回通用权限（避免生成非法的 IN ()）。
     */
    @Select("""
            <script>
            SELECT * FROM sys_permission
            WHERE status = 0 AND deleted = 0
              <if test="menuIds != null and menuIds.size() > 0">
                AND (menu_id IS NULL OR menu_id IN
                <foreach collection="menuIds" item="id" open="(" separator="," close=")">#{id}</foreach>)
              </if>
              <if test="menuIds != null and menuIds.size() == 0">
                AND menu_id IS NULL
              </if>
            ORDER BY sort
            </script>
            """)
    List<SysPermission> selectByMenuIds(@Param("menuIds") Collection<Long> menuIds);
}
