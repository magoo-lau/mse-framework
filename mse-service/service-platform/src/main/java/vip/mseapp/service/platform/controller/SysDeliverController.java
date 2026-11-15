package vip.mseapp.service.platform.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vip.mseapp.core.constant.MseConstants;
import vip.mseapp.core.result.PageResult;
import vip.mseapp.core.result.Result;
import vip.mseapp.service.platform.entity.SysDeliverPackage;
import vip.mseapp.service.platform.service.deliver.DeliverPackageService;

import java.io.IOException;
import java.util.List;

/**
 * 交付附件签发（平台运营端，三段交付管道的「签发」段）：只签发「交付附件包」
 * （license + 租户数据 + 租户变量 + 表结构，RS256 签名内层）——附件包归档下载或直推
 * deliver-studio，由 studio「套件 + 附件」重打包出最终交付包交 deliver-console 安装。
 */
@Validated
@RestController
@RequestMapping("/tenant/deliver")
public class SysDeliverController {

    private final DeliverPackageService deliverService;

    public SysDeliverController(DeliverPackageService deliverService) {
        this.deliverService = deliverService;
    }

    /** 签发附件包（异步任务：立即返回记录，前端按记录轮询进度；全量包须先经 /tenant/license/issue 签发凭证） */
    @SaCheckPermission(value = "deliver:package:export", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PostMapping("/package/export")
    public Result<DeliverPackageService.PackageView> export(
            @RequestBody @Validated DeliverPackageService.ExportRequest request) {
        return Result.ok(deliverService.submitExport(request));
    }

    /** 签发记录（分页；status 0 签发中 1 完成 2 失败，记录行即异步任务） */
    @SaCheckPermission(value = "deliver:package:list", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @GetMapping("/package/page")
    public Result<PageResult<DeliverPackageService.PackageView>> pagePackages(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) Integer status) {
        return Result.ok(deliverService.pagePackages(pageNum, pageSize, tenantId, status));
    }

    /** 签发记录详情（含内层 manifest 快照，审计用） */
    @SaCheckPermission(value = "deliver:package:list", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @GetMapping("/package/{id}")
    public Result<SysDeliverPackage> getPackage(@PathVariable @NotNull Long id) {
        return Result.ok(deliverService.getPackageDetail(id));
    }

    /** 下载附件包（归档 zip，支持 Range 断点续传；浏览器原生下载请走下方 download-url 签发） */
    @SaCheckPermission(value = "deliver:package:download", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @GetMapping("/package/{id}/download")
    public void downloadPackage(@PathVariable @NotNull Long id,
                                jakarta.servlet.http.HttpServletRequest request,
                                jakarta.servlet.http.HttpServletResponse response)
            throws IOException {
        deliverService.downloadPackage(id, request, response);
    }

    /** 签发附件包下载 URL（浏览器原生下载带不了 Authorization 头：短时签名 URL 免会话直下 + Range 断点续传） */
    @SaCheckPermission(value = "deliver:package:download", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @GetMapping("/package/{id}/download-url")
    public Result<DeliverPackageService.DownloadUrlView> packageDownloadUrl(@PathVariable @NotNull Long id) {
        return Result.ok(deliverService.issuePackageDownloadUrl(id));
    }

    /** deliver-studio 直推配置状态（签发对话框展示推送选项用：未配置时仅手动下载上传通道） */
    @SaCheckPermission(value = "deliver:package:list", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @GetMapping("/studio")
    public Result<DeliverPackageService.StudioInfo> studioInfo() {
        return Result.ok(deliverService.studioInfo());
    }

    /** 可选业务模块目录（签发对话框模块多选项；基座模块 system 恒随包不出现在选项） */
    @SaCheckPermission(value = "deliver:package:list", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @GetMapping("/modules")
    public Result<List<String>> availableModules() {
        return Result.ok(deliverService.availableModules());
    }

    /** 签名 URL 下载入口（免会话：token 内嵌 HMAC 签名与有效期，权限已在签发时校验，网关白名单放行本路径） */
    @GetMapping("/download/{token}")
    public void downloadByToken(@PathVariable String token,
                                jakarta.servlet.http.HttpServletRequest request,
                                jakarta.servlet.http.HttpServletResponse response)
            throws IOException {
        deliverService.publicDownload(token, request, response);
    }
}
