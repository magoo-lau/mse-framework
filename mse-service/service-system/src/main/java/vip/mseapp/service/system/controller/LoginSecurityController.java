package vip.mseapp.service.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vip.mseapp.api.system.LoginConfigKeys;
import vip.mseapp.core.constant.MseConstants;
import vip.mseapp.core.context.TenantContext;
import vip.mseapp.core.exception.ServiceException;
import vip.mseapp.core.result.GlobalErrorCode;
import vip.mseapp.core.result.Result;
import vip.mseapp.mail.MseMailSender;
import vip.mseapp.service.system.entity.SysConfig;
import vip.mseapp.service.system.service.SysConfigService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 登录安全设置（「系统管理 → 登录安全设置」专属配置页后端）。
 *
 * <p>表单化管控登录安全参数（底层仍存 sys_config 内置参数，登录链路即时生效）：
 * 图形验证码（模式 + 风控阈值）、双因子认证（模式 + 邮箱动态码通道）、登录策略（多端/同端互斥）。
 * 双端各一组端点：平台端管平台库自身参数行，租户端管本租户覆盖行并可一键恢复默认。
 * 读取语义与登录链路一致（两端独立）：本租户覆盖行 → 本库默认模板行（tenant_id=0 同库副本）→ 代码默认。
 */
@Validated
@RestController
@RequestMapping("/system/login-security")
public class LoginSecurityController {

    /** 受管参数键（固定顺序，前端表单与种子数据共用语义） */
    private static final List<String> KEYS = List.of(
            LoginConfigKeys.CAPTCHA_MODE,
            LoginConfigKeys.CAPTCHA_USER_FAIL_THRESHOLD,
            LoginConfigKeys.CAPTCHA_IP_FAIL_THRESHOLD,
            LoginConfigKeys.TOTP_MODE,
            LoginConfigKeys.EMAIL_OTP,
            LoginConfigKeys.MULTI_DEVICE,
            LoginConfigKeys.SAME_DEVICE_EXCLUSIVE);

    private static final Set<String> CAPTCHA_MODES = Set.of("none", "risk", "always");
    private static final Set<String> TOTP_MODES = Set.of("off", "optional", "required");

    private final SysConfigService configService;
    private final ObjectProvider<MseMailSender> mailSender;

    public LoginSecurityController(SysConfigService configService, ObjectProvider<MseMailSender> mailSender) {
        this.configService = configService;
        this.mailSender = mailSender;
    }

    @SaCheckPermission(value = "system:loginsecurity:query", type = MseConstants.LOGIN_TYPE_TENANT)
    @GetMapping("/config")
    public Result<LoginSecurityVO> config() {
        return Result.ok(build(TenantContext.getRequiredTenantId(), false));
    }

    @SaCheckPermission(value = "system:loginsecurity:platform-query", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @GetMapping("/platform/config")
    public Result<LoginSecurityVO> platformConfig() {
        return Result.ok(build(MseConstants.PLATFORM_TENANT_ID, true));
    }

    @SaCheckPermission(value = "system:loginsecurity:update", type = MseConstants.LOGIN_TYPE_TENANT)
    @PutMapping("/config")
    public Result<Void> save(@RequestBody @Validated LoginSecurityRequest request) {
        save(TenantContext.getRequiredTenantId(), request, false);
        return Result.ok();
    }

    @SaCheckPermission(value = "system:loginsecurity:platform-update", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PutMapping("/platform/config")
    public Result<Void> platformSave(@RequestBody @Validated LoginSecurityRequest request) {
        save(MseConstants.PLATFORM_TENANT_ID, request, true);
        return Result.ok();
    }

    /** 租户端恢复默认：删除本租户覆盖行（本库模板行继续生效，语义即「回退默认」） */
    @SaCheckPermission(value = "system:loginsecurity:update", type = MseConstants.LOGIN_TYPE_TENANT)
    @DeleteMapping("/config")
    public Result<Void> resetOverride() {
        Long tenantId = TenantContext.getRequiredTenantId();
        TenantContext.runAs(tenantId, () -> {
            List<SysConfig> rows = configService.lambdaQuery()
                    .in(SysConfig::getConfigKey, KEYS)
                    .eq(SysConfig::getTenantId, tenantId)
                    .list();
            rows.forEach(row -> configService.removeById(row.getId()));
        });
        return Result.ok();
    }

    // -------------------- 内部实现 --------------------

    private LoginSecurityVO build(Long tenantId, boolean platformManaged) {
        Map<String, String> own = readRows(tenantId);
        // 租户侧默认展示源 = 本库默认模板行（两端独立，不跨读平台库）；平台端视角即平台库自身行
        Map<String, String> defaultRows = platformManaged ? own : readTemplateRows();
        List<String> overridden = new ArrayList<>();
        for (String key : KEYS) {
            String ownValue = own.get(key);
            if (ownValue != null && !ownValue.equals(defaultRows.get(key))) {
                overridden.add(key);
            }
        }
        return new LoginSecurityVO(
                value(own, defaultRows, LoginConfigKeys.CAPTCHA_MODE, "risk"),
                intValue(own, defaultRows, LoginConfigKeys.CAPTCHA_USER_FAIL_THRESHOLD, 3),
                intValue(own, defaultRows, LoginConfigKeys.CAPTCHA_IP_FAIL_THRESHOLD, 10),
                value(own, defaultRows, LoginConfigKeys.TOTP_MODE, "off"),
                boolValue(own, defaultRows, LoginConfigKeys.EMAIL_OTP, false),
                boolValue(own, defaultRows, LoginConfigKeys.MULTI_DEVICE, true),
                boolValue(own, defaultRows, LoginConfigKeys.SAME_DEVICE_EXCLUSIVE, false),
                isMailAvailable(),
                platformManaged,
                overridden);
    }

    private void save(Long tenantId, LoginSecurityRequest request, boolean platform) {
        validate(request);
        Map<String, String> values = new LinkedHashMap<>();
        putIfNotNull(values, LoginConfigKeys.CAPTCHA_MODE, request.captchaMode());
        putIfNotNull(values, LoginConfigKeys.CAPTCHA_USER_FAIL_THRESHOLD,
                request.captchaUserFailThreshold() == null ? null : String.valueOf(request.captchaUserFailThreshold()));
        putIfNotNull(values, LoginConfigKeys.CAPTCHA_IP_FAIL_THRESHOLD,
                request.captchaIpFailThreshold() == null ? null : String.valueOf(request.captchaIpFailThreshold()));
        putIfNotNull(values, LoginConfigKeys.TOTP_MODE, request.totpMode());
        putIfNotNull(values, LoginConfigKeys.EMAIL_OTP,
                request.emailOtp() == null ? null : String.valueOf(request.emailOtp()));
        putIfNotNull(values, LoginConfigKeys.MULTI_DEVICE,
                request.multiDevice() == null ? null : String.valueOf(request.multiDevice()));
        putIfNotNull(values, LoginConfigKeys.SAME_DEVICE_EXCLUSIVE,
                request.sameDeviceExclusive() == null ? null : String.valueOf(request.sameDeviceExclusive()));
        TenantContext.runAs(tenantId, () -> values.forEach((key, value) -> upsert(tenantId, key, value, platform)));
    }

    /** 同键存在即改值（键名建后不可变），否则新建覆盖行：平台域行内置（不可删），租户覆盖行为自定义（可删可回退） */
    private void upsert(Long tenantId, String key, String value, boolean platform) {
        SysConfig existing = configService.lambdaQuery()
                .eq(SysConfig::getConfigKey, key)
                .eq(SysConfig::getTenantId, tenantId)
                .one();
        if (existing != null) {
            SysConfig update = new SysConfig();
            update.setId(existing.getId());
            update.setConfigValue(value);
            configService.updateConfig(update);
            return;
        }
        SysConfig config = new SysConfig();
        config.setTenantId(tenantId);
        config.setConfigName(CONFIG_NAMES.getOrDefault(key, key));
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setConfigType(platform
                ? SysConfig.SYS_CONFIG_TYPE_BUILT_IN
                : SysConfig.SYS_CONFIG_TYPE_CUSTOM);
        config.setRemark("登录安全设置页维护");
        configService.createConfig(config);
    }

    private void validate(LoginSecurityRequest request) {
        if (request.captchaMode() != null && !CAPTCHA_MODES.contains(request.captchaMode())) {
            throw badRequest("图形验证码模式非法（none/risk/always）");
        }
        if (request.totpMode() != null && !TOTP_MODES.contains(request.totpMode())) {
            throw badRequest("双因子认证模式非法（off/optional/required）");
        }
        if (request.captchaUserFailThreshold() != null
                && (request.captchaUserFailThreshold() < 1 || request.captchaUserFailThreshold() > 100)) {
            throw badRequest("同账号失败阈值须在 1~100 之间");
        }
        if (request.captchaIpFailThreshold() != null
                && (request.captchaIpFailThreshold() < 1 || request.captchaIpFailThreshold() > 100)) {
            throw badRequest("同 IP 失败阈值须在 1~100 之间");
        }
    }

    private ServiceException badRequest(String message) {
        return new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), message);
    }

    private Map<String, String> readRows(Long tenantId) {
        return TenantContext.runAs(tenantId, () -> {
            Map<String, String> values = new LinkedHashMap<>();
            configService.lambdaQuery()
                    .in(SysConfig::getConfigKey, KEYS)
                    .eq(SysConfig::getTenantId, tenantId)
                    .list()
                    .forEach(row -> values.putIfAbsent(row.getConfigKey(), row.getConfigValue()));
            return values;
        });
    }

    /**
     * 本库默认模板行（tenant_id=0，与租户同库副本）。保持租户上下文原样执行（不能用 runIgnore：
     * 它会连数据源路由一起跳过，查询落到共享主库而非租户库）——独立库路由下租户插件本就跳过
     * sys_config，等值 0 查询直达本库模板行；共享模式下插件追加本租户条件使 0 行不可见，按无默认处理。
     */
    private Map<String, String> readTemplateRows() {
        Map<String, String> values = new LinkedHashMap<>();
        configService.lambdaQuery()
                .in(SysConfig::getConfigKey, KEYS)
                .eq(SysConfig::getTenantId, MseConstants.PLATFORM_TENANT_ID)
                .list()
                .forEach(row -> values.putIfAbsent(row.getConfigKey(), row.getConfigValue()));
        return values;
    }

    private boolean isMailAvailable() {
        MseMailSender sender = mailSender.getIfAvailable();
        return sender != null && sender.isAvailable();
    }

    /** 有效值：本租户覆盖行 → 本库默认模板行 → 代码默认（与登录链路 LoginPolicyService 同一语义） */
    private static String value(Map<String, String> own, Map<String, String> platformRows, String key, String defaultValue) {
        String v = own.get(key);
        if (v == null) {
            v = platformRows.get(key);
        }
        return v == null ? defaultValue : v;
    }

    private static Integer intValue(Map<String, String> own, Map<String, String> platformRows, String key, int defaultValue) {
        try {
            return Integer.parseInt(value(own, platformRows, key, String.valueOf(defaultValue)));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static Boolean boolValue(Map<String, String> own, Map<String, String> platformRows, String key, boolean defaultValue) {
        return "true".equalsIgnoreCase(value(own, platformRows, key, String.valueOf(defaultValue)));
    }

    private static void putIfNotNull(Map<String, String> values, String key, String value) {
        if (value != null) {
            values.put(key, value);
        }
    }

    // -------------------- 请求/响应 --------------------

    /** 全量字段可空：仅保存传出的字段（前端整表单提交，全部字段照传） */
    public record LoginSecurityRequest(
            String captchaMode,
            Integer captchaUserFailThreshold,
            Integer captchaIpFailThreshold,
            String totpMode,
            Boolean emailOtp,
            Boolean multiDevice,
            Boolean sameDeviceExclusive) {
    }

    public record LoginSecurityVO(
            String captchaMode,
            Integer captchaUserFailThreshold,
            Integer captchaIpFailThreshold,
            String totpMode,
            Boolean emailOtp,
            Boolean multiDevice,
            Boolean sameDeviceExclusive,
            /** SMTP 是否已配置（false 时前端禁用邮箱通道开关并提示） */
            boolean mailAvailable,
            /** 平台端视角（隐藏「恢复平台默认」） */
            boolean platformManaged,
            /** 租户端：本租户已覆盖的参数键（前端高亮显示） */
            List<String> overriddenKeys) {
    }

    /** 新建覆盖行时的参数名（与种子数据一致；平台行已由种子预置，此处主要供租户覆盖行补名） */
    private static final Map<String, String> CONFIG_NAMES = Map.of(
            LoginConfigKeys.CAPTCHA_MODE, "登录安全-图形验证码模式",
            LoginConfigKeys.CAPTCHA_USER_FAIL_THRESHOLD, "登录安全-验证码触发-同账号失败阈值",
            LoginConfigKeys.CAPTCHA_IP_FAIL_THRESHOLD, "登录安全-验证码触发-同IP失败阈值",
            LoginConfigKeys.TOTP_MODE, "登录安全-双因子认证模式",
            LoginConfigKeys.EMAIL_OTP, "登录安全-邮箱动态码通道",
            LoginConfigKeys.MULTI_DEVICE, "登录策略-允许多端登录",
            LoginConfigKeys.SAME_DEVICE_EXCLUSIVE, "登录策略-同端互斥");
}
