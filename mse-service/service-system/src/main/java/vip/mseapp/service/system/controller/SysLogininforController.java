package vip.mseapp.service.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import vip.mseapp.core.constant.MseConstants;
import vip.mseapp.core.result.PageResult;
import vip.mseapp.core.result.Result;
import vip.mseapp.service.system.entity.SysLogininfor;
import vip.mseapp.service.system.service.SysLogininforService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 登录日志（仅租户端）：/system/logininfor/**，查询 + 批量删除 + 清空。
 */
@Validated
@RestController
@RequestMapping("/system/logininfor")
public class SysLogininforController {

    private final SysLogininforService logininforService;

    public SysLogininforController(SysLogininforService logininforService) {
        this.logininforService = logininforService;
    }

    @SaCheckPermission(value = "system:logininfor:list", type = MseConstants.LOGIN_TYPE_TENANT)
    @GetMapping("/page")
    public Result<PageResult<SysLogininfor>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                  @RequestParam(defaultValue = "10") long pageSize,
                                                  @RequestParam(required = false) String username,
                                                  @RequestParam(required = false) String ip,
                                                  @RequestParam(required = false) Integer status) {
        return Result.ok(logininforService.pageLogininfor(pageNum, pageSize, username, ip, status));
    }

    @SaCheckPermission(value = "system:logininfor:delete", type = MseConstants.LOGIN_TYPE_TENANT)
    @DeleteMapping("/{ids}")
    public Result<Void> delete(@PathVariable List<Long> ids) {
        logininforService.deleteByIds(ids);
        return Result.ok();
    }

    @SaCheckPermission(value = "system:logininfor:delete", type = MseConstants.LOGIN_TYPE_TENANT)
    @DeleteMapping("/clean")
    public Result<Void> clean() {
        logininforService.clean();
        return Result.ok();
    }
}
