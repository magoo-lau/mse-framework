package vip.mseapp.service.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import vip.mseapp.core.constant.MseConstants;
import vip.mseapp.core.result.PageResult;
import vip.mseapp.core.result.Result;
import vip.mseapp.service.system.entity.SysConfig;
import vip.mseapp.service.system.service.SysConfigService;
import vip.mseapp.web.log.OperLog;
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

/**
 * 参数配置管理：/system/config/** 租户端操作；/system/config/platform/** 平台端操作（tenant_id=0 域）。
 *
 * <p>按键名取值（/config-key/{key}）是业务公共依赖，仅要求登录即可。
 */
@Validated
@RestController
@RequestMapping("/system/config")
public class SysConfigController {

    private final SysConfigService configService;

    public SysConfigController(SysConfigService configService) {
        this.configService = configService;
    }

    @SaCheckPermission(value = "system:config:list", type = MseConstants.LOGIN_TYPE_TENANT)
    @GetMapping("/page")
    public Result<PageResult<SysConfig>> page(@RequestParam(defaultValue = "1") long pageNum,
                                              @RequestParam(defaultValue = "10") long pageSize,
                                              @RequestParam(required = false) String configName,
                                              @RequestParam(required = false) String configKey,
                                              @RequestParam(required = false) Integer configType) {
        return Result.ok(configService.pageConfigs(pageNum, pageSize, configName, configKey, configType));
    }

    @SaCheckLogin(type = MseConstants.LOGIN_TYPE_TENANT)
    @GetMapping("/config-key/{configKey}")
    public Result<String> configKey(@PathVariable String configKey) {
        return Result.ok(configService.getConfigValueByKey(configKey));
    }

    @OperLog(module = "system:config", type = "新增")
    @SaCheckPermission(value = "system:config:create", type = MseConstants.LOGIN_TYPE_TENANT)
    @PostMapping
    public Result<Long> create(@RequestBody @Validated ConfigSaveRequest request) {
        return Result.ok(doCreate(request));
    }

    @OperLog(module = "system:config", type = "修改")
    @SaCheckPermission(value = "system:config:update", type = MseConstants.LOGIN_TYPE_TENANT)
    @PutMapping
    public Result<Void> update(@RequestBody @Validated ConfigUpdateRequest request) {
        doUpdate(request);
        return Result.ok();
    }

    @OperLog(module = "system:config", type = "删除")
    @SaCheckPermission(value = "system:config:delete", type = MseConstants.LOGIN_TYPE_TENANT)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        configService.deleteConfig(id);
        return Result.ok();
    }

    // -------------------- 平台端（平台管理-参数设置；平台登录 TenantContext=0，租户插件自动隔离到 tenant_id=0 行） --------------------

    @SaCheckPermission(value = "system:config:platform-list", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @GetMapping("/platform/page")
    public Result<PageResult<SysConfig>> platformPage(@RequestParam(defaultValue = "1") long pageNum,
                                                      @RequestParam(defaultValue = "10") long pageSize,
                                                      @RequestParam(required = false) String configName,
                                                      @RequestParam(required = false) String configKey,
                                                      @RequestParam(required = false) Integer configType) {
        return Result.ok(configService.pageConfigs(pageNum, pageSize, configName, configKey, configType));
    }

    @SaCheckLogin(type = MseConstants.LOGIN_TYPE_PLATFORM)
    @GetMapping("/platform/config-key/{configKey}")
    public Result<String> platformConfigKey(@PathVariable String configKey) {
        return Result.ok(configService.getConfigValueByKey(configKey));
    }

    @OperLog(module = "system:config", type = "新增")
    @SaCheckPermission(value = "system:config:platform-create", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PostMapping("/platform")
    public Result<Long> platformCreate(@RequestBody @Validated ConfigSaveRequest request) {
        return Result.ok(doCreate(request));
    }

    @OperLog(module = "system:config", type = "修改")
    @SaCheckPermission(value = "system:config:platform-update", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PutMapping("/platform")
    public Result<Void> platformUpdate(@RequestBody @Validated ConfigUpdateRequest request) {
        doUpdate(request);
        return Result.ok();
    }

    @OperLog(module = "system:config", type = "删除")
    @SaCheckPermission(value = "system:config:platform-delete", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @DeleteMapping("/platform/{id}")
    public Result<Void> platformDelete(@PathVariable Long id) {
        configService.deleteConfig(id);
        return Result.ok();
    }

    // -------------------- 请求体与公共实现 --------------------

    private Long doCreate(ConfigSaveRequest request) {
        SysConfig config = new SysConfig();
        config.setConfigName(request.configName());
        config.setConfigKey(request.configKey());
        config.setConfigValue(request.configValue());
        // 内置标识只能由系统种子定义（业务按键名依赖、不可删除），用户新建一律自定义
        config.setConfigType(SysConfig.SYS_CONFIG_TYPE_CUSTOM);
        config.setRemark(request.remark());
        return configService.createConfig(config);
    }

    private void doUpdate(ConfigUpdateRequest request) {
        SysConfig config = new SysConfig();
        config.setId(request.id());
        config.setConfigName(request.configName());
        config.setConfigValue(request.configValue());
        config.setRemark(request.remark());
        configService.updateConfig(config);
    }

    public record ConfigSaveRequest(
            @NotBlank(message = "参数名称不能为空") String configName,
            @NotBlank(message = "参数键名不能为空") String configKey,
            @NotBlank(message = "参数键值不能为空") String configValue,
            Integer configType,
            String remark) {
    }

    public record ConfigUpdateRequest(
            @NotNull(message = "参数编号不能为空") Long id,
            @NotBlank(message = "参数名称不能为空") String configName,
            @NotBlank(message = "参数键值不能为空") String configValue,
            String remark) {
    }
}
