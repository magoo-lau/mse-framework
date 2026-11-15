package vip.mseapp.service.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import vip.mseapp.core.constant.MseConstants;
import vip.mseapp.core.result.PageResult;
import vip.mseapp.core.result.Result;
import vip.mseapp.web.log.OperLog;
import vip.mseapp.service.system.entity.SysMenu;
import vip.mseapp.service.system.entity.SysRole;
import vip.mseapp.service.system.service.SysRoleService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色管理：/system/role/** 租户内；/system/role/platform/** 平台端（tenant_id=0 域）。
 * 授权内容经服务层按当前端域校验（租户端 ⊆ 套餐域，平台端 ⊆ 平台域）。
 */
@Validated
@RestController
@RequestMapping("/system/role")
public class SysRoleController {

    private final SysRoleService roleService;

    public SysRoleController(SysRoleService roleService) {
        this.roleService = roleService;
    }

    /** 客户端口径归一：缺省 PC（存量界面不传参零改动），非法值回落 PC */
    private int resolveClient(Integer client) {
        return client != null && client == SysMenu.CLIENT_MOBILE ? SysMenu.CLIENT_MOBILE : SysMenu.CLIENT_PC;
    }

    // -------------------- 租户内 --------------------

    @SaCheckPermission(value = "system:role:list", type = MseConstants.LOGIN_TYPE_TENANT)
    @GetMapping("/page")
    public Result<PageResult<SysRole>> page(@RequestParam(defaultValue = "1") long pageNum,
                                            @RequestParam(defaultValue = "10") long pageSize,
                                            @RequestParam(required = false) String name,
                                            @RequestParam(required = false) Integer status) {
        return Result.ok(roleService.pageRoles(pageNum, pageSize, name, status));
    }

    @SaCheckPermission(value = "system:role:list", type = MseConstants.LOGIN_TYPE_TENANT)
    @GetMapping("/list")
    public Result<List<SysRole>> list() {
        return Result.ok(roleService.listRoles());
    }

    @SaCheckPermission(value = "system:role:query", type = MseConstants.LOGIN_TYPE_TENANT)
    @GetMapping("/{id}")
    public Result<SysRole> get(@PathVariable Long id) {
        return Result.ok(roleService.getById(id));
    }

    @SaCheckPermission(value = "system:role:query", type = MseConstants.LOGIN_TYPE_TENANT)
    @GetMapping("/{id}/grants")
    public Result<SysRoleService.RoleGrants> grants(@PathVariable Long id) {
        return Result.ok(roleService.getRoleGrants(id));
    }

    @SaCheckPermission(value = "system:role:assign", type = MseConstants.LOGIN_TYPE_TENANT)
    @GetMapping("/assign-options")
    public Result<SysRoleService.AssignOptions> assignOptions(@RequestParam(required = false) Integer client) {
        return Result.ok(roleService.getAssignOptions(resolveClient(client)));
    }

    @OperLog(module = "system:role", type = "新增")
    @SaCheckPermission(value = "system:role:create", type = MseConstants.LOGIN_TYPE_TENANT)
    @PostMapping
    public Result<Long> create(@RequestBody @Validated RoleSaveRequest request) {
        SysRole role = request.toEntity();
        role.setId(null);
        return Result.ok(roleService.createRole(role, request.menuIds(), request.permissionIds(), request.deptIds()));
    }

    @OperLog(module = "system:role", type = "修改")
    @SaCheckPermission(value = "system:role:update", type = MseConstants.LOGIN_TYPE_TENANT)
    @PutMapping
    public Result<Void> update(@RequestBody @Validated RoleSaveRequest request) {
        SysRole role = request.toEntity();
        roleService.updateRole(role, request.menuIds(), request.permissionIds(), request.deptIds());
        return Result.ok();
    }

    @OperLog(module = "system:role", type = "删除")
    @SaCheckPermission(value = "system:role:delete", type = MseConstants.LOGIN_TYPE_TENANT)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.deleteRole(id);
        return Result.ok();
    }

    // -------------------- 平台端（tenant_id=0 域） --------------------

    @SaCheckPermission(value = "system:role:platform-list", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @GetMapping("/platform/page")
    public Result<PageResult<SysRole>> platformPage(@RequestParam(defaultValue = "1") long pageNum,
                                                    @RequestParam(defaultValue = "10") long pageSize,
                                                    @RequestParam(required = false) String name,
                                                    @RequestParam(required = false) Integer status) {
        return Result.ok(roleService.pageRoles(pageNum, pageSize, name, status));
    }

    @SaCheckPermission(value = "system:role:platform-list", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @GetMapping("/platform/list")
    public Result<List<SysRole>> platformList() {
        return Result.ok(roleService.listRoles());
    }

    @SaCheckPermission(value = "system:role:platform-query", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @GetMapping("/platform/{id}")
    public Result<SysRole> platformGet(@PathVariable Long id) {
        return Result.ok(roleService.getById(id));
    }

    @SaCheckPermission(value = "system:role:platform-query", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @GetMapping("/platform/{id}/grants")
    public Result<SysRoleService.RoleGrants> platformGrants(@PathVariable Long id) {
        return Result.ok(roleService.getRoleGrants(id));
    }

    @SaCheckPermission(value = "system:role:platform-assign", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @GetMapping("/platform/assign-options")
    public Result<SysRoleService.AssignOptions> platformAssignOptions(@RequestParam(required = false) Integer client) {
        return Result.ok(roleService.getAssignOptions(resolveClient(client)));
    }

    @SaCheckPermission(value = "system:role:platform-create", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PostMapping("/platform")
    public Result<Long> platformCreate(@RequestBody @Validated RoleSaveRequest request) {
        SysRole role = request.toEntity();
        role.setId(null);
        return Result.ok(roleService.createRole(role, request.menuIds(), request.permissionIds(), request.deptIds()));
    }

    @SaCheckPermission(value = "system:role:platform-update", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PutMapping("/platform")
    public Result<Void> platformUpdate(@RequestBody @Validated RoleSaveRequest request) {
        SysRole role = request.toEntity();
        roleService.updateRole(role, request.menuIds(), request.permissionIds(), request.deptIds());
        return Result.ok();
    }

    // -------------------- 请求体 --------------------

    public record RoleSaveRequest(
            Long id,
            @NotBlank(message = "角色名不能为空") String name,
            @NotBlank(message = "角色编码不能为空") String code,
            Integer sort,
            /** 数据范围 1 全部 2 自定义 3 本部门 4 本部门及以下 5 仅本人 */
            Integer dataScope,
            Integer status,
            String remark,
            List<Long> menuIds,
            List<Long> permissionIds,
            /** dataScope=2 自定义时的部门集合 */
            List<Long> deptIds) {

        SysRole toEntity() {
            SysRole role = new SysRole();
            role.setId(id);
            role.setName(name);
            role.setCode(code);
            role.setSort(sort == null ? 0 : sort);
            role.setDataScope(dataScope);
            role.setStatus(status == null ? 0 : status);
            role.setRemark(remark);
            return role;
        }
    }
}
