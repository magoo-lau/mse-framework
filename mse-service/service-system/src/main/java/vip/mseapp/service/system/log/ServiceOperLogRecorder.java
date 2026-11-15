package vip.mseapp.service.system.log;

import vip.mseapp.core.context.TenantContext;
import vip.mseapp.service.system.entity.SysOperLog;
import vip.mseapp.service.system.mapper.SysOperLogMapper;
import vip.mseapp.service.system.service.SysOperLogService;
import vip.mseapp.web.log.OperLogEvent;
import vip.mseapp.web.log.OperLogRecorder;

/**
 * 操作日志落库实现（common-web 切面的 SPI）。
 *
 * <p>切面在工作线程回调，租户上下文不可继承，按事件携带的租户 runAs 后写入。
 */
public class ServiceOperLogRecorder implements OperLogRecorder {

    private final SysOperLogMapper operLogMapper;

    public ServiceOperLogRecorder(SysOperLogMapper operLogMapper) {
        this.operLogMapper = operLogMapper;
    }

    @Override
    public void record(OperLogEvent event) {
        SysOperLog operLog = new SysOperLog();
        operLog.setModule(event.getModule());
        operLog.setOperType(event.getOperType());
        operLog.setMethod(event.getMethod());
        operLog.setRequestMethod(event.getRequestMethod());
        operLog.setOperUrl(event.getOperUrl());
        operLog.setOperParam(event.getOperParam());
        operLog.setOperUserId(event.getOperUserId());
        operLog.setOperUsername(event.getOperUsername());
        operLog.setOperIp(event.getOperIp());
        operLog.setCostMs(event.getCostMs());
        operLog.setStatus(event.getStatus() == null ? 0 : event.getStatus());
        operLog.setErrorMsg(event.getErrorMsg());
        operLog.setOperTime(event.getOperTime());
        TenantContext.runAs(event.getTenantId() == null ? 0L : event.getTenantId(),
                () -> operLogMapper.insert(operLog));
    }
}
