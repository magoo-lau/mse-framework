package vip.mseapp.service.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import vip.mseapp.core.exception.ServiceException;
import vip.mseapp.service.system.entity.SysMenu;
import vip.mseapp.service.system.entity.SysMicroApp;
import vip.mseapp.service.system.mapper.SysMenuMapper;
import vip.mseapp.service.system.mapper.SysMicroAppMapper;
import vip.mseapp.service.system.mapper.SysRoleMapper;
import vip.mseapp.service.system.service.SysMenuService;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SysMenuServiceImpl extends CrudRepository<SysMenuMapper, SysMenu> implements SysMenuService {

    private final SysRoleMapper roleMapper;
    private final SysMicroAppMapper microAppMapper;

    public SysMenuServiceImpl(SysRoleMapper roleMapper, SysMicroAppMapper microAppMapper) {
        this.roleMapper = roleMapper;
        this.microAppMapper = microAppMapper;
    }

    @Override
    public List<MenuTreeNode> getMenuTree() {
        return buildTree(list(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getStatus, 0)
                .orderByAsc(SysMenu::getSort)));
    }

    @Override
    public List<MenuTreeNode> getMenuTreeByScope(int menuScope, int client) {
        return buildTree(baseMapper.selectByScopeAndClient(menuScope, client));
    }

    @Override
    public List<SysMenu> listByScopeMerged(int menuScope, int client) {
        // 目录真源在会话库（平台会话=平台库、租户会话=租户库自持副本），单库直读
        return baseMapper.selectByScopeAndClient(menuScope, client);
    }

    @Override
    public List<MenuTreeNode> getMenuTreeInDomain(int menuScope, int client, Set<Long> domainMenuIds) {
        List<SysMenu> menus = baseMapper.selectByScopeAndClient(menuScope, client).stream()
                .filter(menu -> domainMenuIds != null && domainMenuIds.contains(menu.getId()))
                .toList();
        return buildTree(menus);
    }

    @Override
    public List<MenuTreeNode> buildTree(List<SysMenu> menus) {
        Map<Long, List<SysMenu>> byParent = menus.stream()
                .sorted(Comparator.comparing(SysMenu::getSort,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.groupingBy(m -> m.getParentId() == null ? 0L : m.getParentId()));
        return byParent.getOrDefault(0L, List.of()).stream()
                .map(menu -> new MenuTreeNode(menu, buildChildren(menu.getId(), byParent)))
                .toList();
    }

    @Override
    public void validate(SysMenu menu) {
        int type = menu.getType() == null ? 0 : menu.getType();
        if (type < SysMenu.TYPE_DIR || type > SysMenu.TYPE_MICRO) {
            throw new ServiceException(400, "菜单类型非法（1 目录 / 2 菜单 / 3 子应用页面）");
        }
        int scope = menu.getMenuScope() == null ? 0 : menu.getMenuScope();
        if (scope < SysMenu.SCOPE_PLATFORM || scope > SysMenu.SCOPE_BOTH) {
            throw new ServiceException(400, "可见域非法（1 平台端 / 2 租户端 / 3 双端）");
        }
        int client = menu.getClient() == null ? SysMenu.CLIENT_PC : menu.getClient();
        if (client != SysMenu.CLIENT_PC && client != SysMenu.CLIENT_MOBILE) {
            throw new ServiceException(400, "客户端非法（1 PC / 2 移动端）");
        }
        menu.setClient(client);
        if (type == SysMenu.TYPE_MICRO) {
            if (menu.getMicroName() == null || menu.getMicroName().isBlank()) {
                throw new ServiceException(400, "子应用页面须指定所属子应用");
            }
            if (menu.getPath() == null || !menu.getPath().startsWith("/")) {
                // path 为子应用内部路由，绝对路径才能与 /sub/{name} 前缀直接拼接
                throw new ServiceException(400, "子应用页面的路由地址须为以 / 开头的内部路由（如 /home）");
            }
            // 所属子应用必须已注册，避免侧边栏挂出无法加载的死菜单
            Long count = microAppMapper.selectCount(new LambdaQueryWrapper<SysMicroApp>()
                    .eq(SysMicroApp::getName, menu.getMicroName()));
            if (count == null || count == 0) {
                throw new ServiceException(400, "所属子应用不存在：" + menu.getMicroName());
            }
        } else {
            // 非子应用页面不允许残留 micro_name，防脏数据
            menu.setMicroName(null);
        }
    }

    @Override
    public Set<String> getRoleCodesByUserId(Long userId) {
        return new HashSet<>(roleMapper.selectRoleCodesByUserId(userId));
    }

    private List<MenuTreeNode> buildChildren(Long parentId, Map<Long, List<SysMenu>> byParent) {
        return byParent.getOrDefault(parentId, List.of()).stream()
                .map(menu -> new MenuTreeNode(menu, buildChildren(menu.getId(), byParent)))
                .toList();
    }
}
