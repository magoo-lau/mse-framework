package vip.mseapp.service.system.rpc;

import vip.mseapp.api.system.RemoteLogService;
import vip.mseapp.api.system.dto.LoginLogDTO;
import vip.mseapp.api.system.dto.OperLogDTO;
import vip.mseapp.core.annotation.RpcService;
import vip.mseapp.core.context.TenantContext;
import vip.mseapp.service.system.entity.SysLogininfor;
import vip.mseapp.service.system.entity.SysOperLog;
import vip.mseapp.service.system.mapper.SysOperLogMapper;
import vip.mseapp.service.system.service.SysLogininforService;

import java.time.LocalDateTime;

/**
 * 日志服务契约实现：登录日志落库（显式租户语义，见 {@link SysLogininforService#record}）；
 * 操作日志为 starter-bridge 的 RPC 落库通道（业务微服务经 @OperLog 切面投递）。
 */
@RpcService(RemoteLogService.class)
public class RemoteLogServiceImpl implements RemoteLogService {

    private final SysLogininforService logininforService;
    private final SysOperLogMapper operLogMapper;

    public RemoteLogServiceImpl(SysLogininforService logininforService, SysOperLogMapper operLogMapper) {
        this.logininforService = logininforService;
        this.operLogMapper = operLogMapper;
    }

    @Override
    public void recordLoginLog(LoginLogDTO log) {
        SysLogininfor entity = new SysLogininfor();
        entity.setTenantId(log.getTenantId());
        entity.setUsername(log.getUsername());
        entity.setIp(log.getIp());
        entity.setBrowser(log.getBrowser());
        entity.setOs(log.getOs());
        entity.setStatus(log.getStatus() == null ? 0 : log.getStatus());
        entity.setMsg(log.getMsg());
        entity.setLoginTime(LocalDateTime.now());
        logininforService.record(entity);
    }

    @Override
    public void recordOperLog(OperLogDTO log) {
        SysOperLog operLog = new SysOperLog();
        operLog.setModule(log.getModule());
        operLog.setOperType(log.getOperType());
        operLog.setMethod(log.getMethod());
        operLog.setRequestMethod(log.getRequestMethod());
        operLog.setOperUrl(log.getOperUrl());
        operLog.setOperParam(log.getOperParam());
        operLog.setOperUserId(log.getOperUserId());
        operLog.setOperUsername(log.getOperUsername());
        operLog.setOperIp(log.getOperIp());
        operLog.setCostMs(log.getCostMs());
        operLog.setStatus(log.getStatus() == null ? 0 : log.getStatus());
        operLog.setErrorMsg(log.getErrorMsg());
        operLog.setOperTime(log.getOperTime());
        TenantContext.runAs(log.getTenantId() == null ? 0L : log.getTenantId(),
                () -> operLogMapper.insert(operLog));
    }
}
