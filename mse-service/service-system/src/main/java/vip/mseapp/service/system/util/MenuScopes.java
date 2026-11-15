package vip.mseapp.service.system.util;

import vip.mseapp.service.system.entity.SysMenu;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 租户域菜单集合工具：id 提取与祖先链闭合。
 */
public final class MenuScopes {

    private MenuScopes() {
    }

    /** 行集 → id 集合 */
    public static Set<Long> ids(List<SysMenu> menus) {
        Set<Long> ids = new HashSet<>();
        for (SysMenu menu : menus) {
            ids.add(menu.getId());
        }
        return ids;
    }

    /**
     * 祖先链闭合：把域内菜单的父目录逐级补进域（父 id 必须命中真实菜单行，0/缺失即止，环安全）。
     * 套餐/凭证只圈叶子时（历史坏数据或套餐界面跨页签收割缺陷），目录缺失会让 buildTree 组不出根——
     * 整棵树退化为空列表，租户端表现为侧边栏全空。返回新集合，不入参变更。
     */
    public static Set<Long> withAncestorDirs(Set<Long> domainIds, List<SysMenu> scopeMenus) {
        Map<Long, Long> parentOf = new HashMap<>();
        for (SysMenu menu : scopeMenus) {
            if (menu.getParentId() != null) {
                parentOf.put(menu.getId(), menu.getParentId());
            }
        }
        Set<Long> result = new HashSet<>(domainIds);
        for (Long id : domainIds) {
            Long parent = parentOf.get(id);
            while (parent != null && parent != 0L && result.add(parent)) {
                parent = parentOf.get(parent);
            }
        }
        return result;
    }
}
