package vip.mseapp.job.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vip.mseapp.core.constant.MseConstants;
import vip.mseapp.core.result.Result;
import vip.mseapp.job.dispatch.JobDispatchCenter;

import java.util.Map;

/**
 * mse-job 观测端点（平台登录即可读，只读无操作）：派发节拍运行态/累计派发/最近派发明细。
 * 部署形态：经网关 {@code /job/**} → lb://mse-job。
 */
@RestController
@RequestMapping("/job")
public class JobStateController {

    private final JobDispatchCenter dispatchCenter;

    public JobStateController(JobDispatchCenter dispatchCenter) {
        this.dispatchCenter = dispatchCenter;
    }

    @SaCheckLogin(type = MseConstants.LOGIN_TYPE_PLATFORM)
    @GetMapping("/state")
    public Result<Map<String, Object>> state() {
        return Result.ok(dispatchCenter.state());
    }
}
