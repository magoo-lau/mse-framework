package vip.mseapp.service.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckOr;
import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vip.mseapp.core.constant.MseConstants;
import vip.mseapp.core.result.Result;
import vip.mseapp.service.system.entity.SysHomeTemplate;
import vip.mseapp.service.system.entity.SysHomeUserSetting;
import vip.mseapp.service.system.service.SysHomeTemplateService;
import vip.mseapp.service.system.service.SysHomeUserSettingService;
import vip.mseapp.web.log.OperLog;

/**
 * 门户首页：模板配置（管理端）+ 用户个性化（使用端）。
 *
 * <p>平台与租户模板两端物理分库、相互独立（平台库份=平台模板，租户库份=本租户模板），
 * 无配置时前端回退内置默认；用户个性化跟随本端生效模板。
 * 管理端点按登录端定位范围（平台端写平台库 / 租户端写本租户库），
 * 同一权限码在两端分别授给各自的角色（platform-* 后缀区分，见 sys_permission 种子）。
 */
@Validated
@RestController
@RequestMapping("/system/home")
public class SysHomeController {

    private final SysHomeTemplateService templateService;
    private final SysHomeUserSettingService userSettingService;

    public SysHomeController(SysHomeTemplateService templateService, SysHomeUserSettingService userSettingService) {
        this.templateService = templateService;
        this.userSettingService = userSettingService;
    }

    // ===== 管理端：首页模板（「首页配置」页面，双端复用同一组件） =====

    @SaCheckOr(permission = {
            @SaCheckPermission(value = "system:home-template:list", type = MseConstants.LOGIN_TYPE_TENANT),
            @SaCheckPermission(value = "system:home-template:platform-list", type = MseConstants.LOGIN_TYPE_PLATFORM)
    })
    @GetMapping("/template")
    public Result<SysHomeTemplateService.TemplateEditVO> template() {
        return Result.ok(templateService.getForEdit());
    }

    @OperLog(module = "system:home-template", type = "保存")
    @SaCheckOr(permission = {
            @SaCheckPermission(value = "system:home-template:save", type = MseConstants.LOGIN_TYPE_TENANT),
            @SaCheckPermission(value = "system:home-template:platform-save", type = MseConstants.LOGIN_TYPE_PLATFORM)
    })
    @PutMapping("/template")
    public Result<Void> saveTemplate(@RequestBody @Validated TemplateSaveRequest request) {
        templateService.save(request.name(), request.layoutJson(), request.status());
        return Result.ok();
    }

    @OperLog(module = "system:home-template", type = "恢复默认")
    @SaCheckOr(permission = {
            @SaCheckPermission(value = "system:home-template:reset", type = MseConstants.LOGIN_TYPE_TENANT),
            @SaCheckPermission(value = "system:home-template:platform-reset", type = MseConstants.LOGIN_TYPE_PLATFORM)
    })
    @DeleteMapping("/template")
    public Result<Void> resetTemplate() {
        templateService.reset();
        return Result.ok();
    }

    // ===== 使用端：首页渲染数据 + 用户个性化（双端登录皆可） =====

    @SaCheckOr(login = {
            @SaCheckLogin(type = MseConstants.LOGIN_TYPE_TENANT),
            @SaCheckLogin(type = MseConstants.LOGIN_TYPE_PLATFORM)
    })
    @GetMapping("/portal")
    public Result<PortalVO> portal() {
        SysHomeUserSetting setting = userSettingService.getMySetting();
        return Result.ok(new PortalVO(templateService.resolveEffective(), setting));
    }

    @SaCheckOr(login = {
            @SaCheckLogin(type = MseConstants.LOGIN_TYPE_TENANT),
            @SaCheckLogin(type = MseConstants.LOGIN_TYPE_PLATFORM)
    })
    @PutMapping("/user-setting")
    public Result<Void> saveUserSetting(@RequestBody @Validated UserSettingSaveRequest request) {
        userSettingService.saveMySetting(request.layoutJson());
        return Result.ok();
    }

    @SaCheckOr(login = {
            @SaCheckLogin(type = MseConstants.LOGIN_TYPE_TENANT),
            @SaCheckLogin(type = MseConstants.LOGIN_TYPE_PLATFORM)
    })
    @DeleteMapping("/user-setting")
    public Result<Void> resetUserSetting() {
        userSettingService.resetMySetting();
        return Result.ok();
    }

    public record TemplateSaveRequest(String name,
                                      @NotBlank(message = "布局 JSON 不能为空") String layoutJson,
                                      Integer status) {
    }

    public record UserSettingSaveRequest(@NotBlank(message = "布局 JSON 不能为空") String layoutJson) {
    }

    /** 首页渲染数据：生效模板（可为 null=回退前端内置默认）+ 本人个性化设置（可为 null=跟随模板） */
    public record PortalVO(SysHomeTemplate template, SysHomeUserSetting userSetting) {
    }
}
