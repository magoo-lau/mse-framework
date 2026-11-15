package vip.mseapp.starter.tenant;

import cn.dev33.satoken.annotation.SaCheckPermission;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import vip.mseapp.core.constant.MseConstants;
import vip.mseapp.core.exception.ServiceException;
import vip.mseapp.core.result.Result;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * 离线交付导入端点（standalone 私有环境，租户端超管）：部署包上传 → 验签/计划预览 → 应用。
 * 仅随 tenant-starter 装配（在线形态无本控制器）；随包 license.token 落盘后 60s 内热生效。
 */
@Validated
@RestController
@RequestMapping("/deliver/import")
public class StandaloneImportController {

    private final DeliverImportService importService;

    public StandaloneImportController(DeliverImportService importService) {
        this.importService = importService;
    }

    /** 环境信息（导入页展示：本机租户/凭证状态/公钥是否就绪） */
    @SaCheckPermission(value = "deliver:package:import", type = MseConstants.LOGIN_TYPE_TENANT)
    @GetMapping("/info")
    public Result<Map<String, Object>> info() {
        return Result.ok(Map.of(
                "standalone", true,
                "tenantCode", importService.localTenantCode(),
                "licenseState", importService.licenseState(),
                "publicKeyConfigured", importService.publicKeyConfigured()));
    }

    /** 上传部署包：验签 + 导入计划预览（不落库） */
    @SaCheckPermission(value = "deliver:package:import", type = MseConstants.LOGIN_TYPE_TENANT)
    @PostMapping("/plan")
    public Result<DeliverImportService.ImportPlan> plan(@RequestParam("file") MultipartFile file) {
        Path temp = toTempFile(file);
        try {
            return Result.ok(importService.plan(temp));
        } catch (IOException e) {
            throw new ServiceException(400, "部署包读取失败: " + e.getMessage());
        } catch (Exception e) {
            throw new ServiceException(400, e.getMessage() == null ? "部署包校验失败" : e.getMessage());
        } finally {
            deleteQuietly(temp);
        }
    }

    /** 上传部署包：验签 + 应用（license 落盘 + 数据装载，每表独立事务可重试） */
    @SaCheckPermission(value = "deliver:package:import", type = MseConstants.LOGIN_TYPE_TENANT)
    @PostMapping("/apply")
    public Result<DeliverImportService.ApplyResult> apply(@RequestParam("file") MultipartFile file) {
        Path temp = toTempFile(file);
        try {
            return Result.ok(importService.apply(temp, file.getOriginalFilename()));
        } catch (IOException e) {
            throw new ServiceException(400, "部署包读取失败: " + e.getMessage());
        } catch (Exception e) {
            throw new ServiceException(400, e.getMessage() == null ? "部署包导入失败" : e.getMessage());
        } finally {
            deleteQuietly(temp);
        }
    }

    /** 导入历史（审计，最近 50 条） */
    @SaCheckPermission(value = "deliver:package:import", type = MseConstants.LOGIN_TYPE_TENANT)
    @GetMapping("/records")
    public Result<List<Map<String, Object>>> records() {
        return Result.ok(importService.recentRecords());
    }

    private Path toTempFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException(400, "请上传部署包文件");
        }
        try {
            Path temp = Files.createTempFile("deliver-import-", ".zip");
            file.transferTo(temp);
            return temp;
        } catch (IOException e) {
            throw new ServiceException(500, "上传文件暂存失败: " + e.getMessage());
        }
    }

    private static void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // 临时文件清理失败不影响响应
        }
    }
}
