package vip.mseapp.service.platform.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vip.mseapp.core.constant.MseConstants;
import vip.mseapp.core.result.Result;
import vip.mseapp.service.platform.entity.SysTenantLicense;
import vip.mseapp.service.platform.service.TenantLicenseService;

import java.util.List;

/**
 * 授权凭证签发（平台运营端，私有部署授权）：租户管理页「导出授权凭证」——
 * 签发 RS256 凭证（token 一次性返回，导出 .license 文件交付租户）+ 签发记录审计。
 */
@Validated
@RestController
@RequestMapping("/tenant/license")
public class SysTenantLicenseController {

    private final TenantLicenseService licenseService;

    public SysTenantLicenseController(TenantLicenseService licenseService) {
        this.licenseService = licenseService;
    }

    /** 签发授权凭证（有效期默认 365 天；token 仅本次返回） */
    @SaCheckPermission(value = "tenant:tenant:update", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PostMapping("/issue")
    public Result<TenantLicenseService.IssuedLicense> issue(@RequestBody @Validated IssueRequest request) {
        return Result.ok(licenseService.issue(request.tenantId(),
                request.days() == null ? 365 : request.days(), request.menuIds()));
    }

    /** 签发记录（审计台账） */
    @SaCheckPermission(value = "tenant:tenant:query", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @GetMapping("/list")
    public Result<List<SysTenantLicense>> list(@RequestParam(required = false) Long tenantId) {
        return Result.ok(licenseService.list(tenantId));
    }

    /** 撤销签发记录（审计标记；续期拒绝依据） */
    @SaCheckPermission(value = "tenant:tenant:update", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PutMapping("/{licenseId}/revoke")
    public Result<Void> revoke(@PathVariable @NotBlank String licenseId) {
        licenseService.revoke(licenseId);
        return Result.ok();
    }

    public record IssueRequest(
            @NotNull(message = "tenantId 不能为空") Long tenantId,
            @Min(value = 1, message = "有效期至少 1 天") @Max(value = 3650, message = "有效期至多 3650 天") Integer days,
            /** 随包导出菜单（导出对话框树勾选；空 = 回退租户套餐） */
            java.util.List<Long> menuIds) {
    }
}
