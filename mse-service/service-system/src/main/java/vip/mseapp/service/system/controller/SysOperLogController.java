package vip.mseapp.service.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import vip.mseapp.core.constant.MseConstants;
import vip.mseapp.core.result.PageResult;
import vip.mseapp.core.result.Result;
import vip.mseapp.service.system.entity.SysOperLog;
import vip.mseapp.service.system.service.SysOperLogService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 操作日志（仅租户端）：/system/operlog/**，查询 + 批量删除 + 清空。
 */
@Validated
@RestController
@RequestMapping("/system/operlog")
public class SysOperLogController {

    private final SysOperLogService operLogService;

    public SysOperLogController(SysOperLogService operLogService) {
        this.operLogService = operLogService;
    }

    @SaCheckPermission(value = "system:operlog:list", type = MseConstants.LOGIN_TYPE_TENANT)
    @GetMapping("/page")
    public Result<PageResult<SysOperLog>> page(@RequestParam(defaultValue = "1") long pageNum,
                                               @RequestParam(defaultValue = "10") long pageSize,
                                               @RequestParam(required = false) String module,
                                               @RequestParam(required = false) String operUsername,
                                               @RequestParam(required = false) String operType,
                                               @RequestParam(required = false) Integer status) {
        return Result.ok(operLogService.pageOperLogs(pageNum, pageSize, module, operUsername, operType, status));
    }

    @SaCheckPermission(value = "system:operlog:list", type = MseConstants.LOGIN_TYPE_TENANT)
    @GetMapping("/{id}")
    public Result<SysOperLog> get(@PathVariable Long id) {
        return Result.ok(operLogService.getById(id));
    }

    @SaCheckPermission(value = "system:operlog:delete", type = MseConstants.LOGIN_TYPE_TENANT)
    @DeleteMapping("/{ids}")
    public Result<Void> delete(@PathVariable List<Long> ids) {
        operLogService.deleteByIds(ids);
        return Result.ok();
    }

    @SaCheckPermission(value = "system:operlog:delete", type = MseConstants.LOGIN_TYPE_TENANT)
    @DeleteMapping("/clean")
    public Result<Void> clean() {
        operLogService.clean();
        return Result.ok();
    }
}
