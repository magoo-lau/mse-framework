package vip.mseapp.service.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import vip.mseapp.service.system.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /** 查询用户可见菜单（构建前端路由；sys_menu 为全局表，其余表租户条件由插件追加；
     * rm/ur 为逻辑删除表，必须过滤 deleted——否则被回收的菜单授权仍会联出） */
    @Select("""
            SELECT DISTINCT m.* FROM sys_menu m
            JOIN sys_role_menu rm ON rm.menu_id = m.id AND rm.deleted = 0
            JOIN sys_user_role ur ON ur.role_id = rm.role_id AND ur.deleted = 0
            JOIN sys_role r ON r.id = ur.role_id
            WHERE ur.user_id = #{userId} AND m.status = 0 AND m.deleted = 0
              AND r.status = 0 AND r.deleted = 0
              AND m.client = #{client}
            ORDER BY m.sort
            """)
    List<SysMenu> selectMenusByUserId(@Param("userId") Long userId, @Param("client") Integer client);

    /** 查询指定可见域的全部菜单（含双端：menu_scope IN (本端, 3)；双 client 全量——
     * 平台域枚举/租户域枚举等"域"口径不受端分离影响，端过滤在树/可见性层叠加） */
    @Select("""
            SELECT * FROM sys_menu
            WHERE menu_scope IN (#{menuScope}, 3) AND status = 0 AND deleted = 0
            ORDER BY sort
            """)
    List<SysMenu> selectByScope(@Param("menuScope") Integer menuScope);

    /** 查询指定可见域 + 客户端的菜单（含双端：menu_scope IN (本端, 3)；管理界面树/授权选项树/用户可见性用） */
    @Select("""
            SELECT * FROM sys_menu
            WHERE menu_scope IN (#{menuScope}, 3) AND client = #{client} AND status = 0 AND deleted = 0
            ORDER BY sort
            """)
    List<SysMenu> selectByScopeAndClient(@Param("menuScope") Integer menuScope, @Param("client") Integer client);
}
