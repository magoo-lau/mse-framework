package vip.mseapp.rpc.bridge;

import vip.mseapp.api.system.RemoteLogService;
import vip.mseapp.api.system.dto.OperLogDTO;
import vip.mseapp.core.annotation.RpcReference;
import vip.mseapp.web.log.OperLogEvent;
import vip.mseapp.web.log.OperLogRecorder;

/**
 * 操作日志落库的 RPC 实现（消费 mse-system 的 {@link RemoteLogService#recordOperLog}）。
 *
 * <p>面向使用 @OperLog 但未装配 service-system 的业务微服务：切面事件在本服务映射为
 * 契约 DTO 后投递 mse-system 落库 sys_oper_log，租户归属随事件携带（服务端显式 runAs）。
 * 基座本地实现（service-system）在 classpath 时本类不装配。
 */
public class RpcOperLogRecorder implements OperLogRecorder {

    @RpcReference
    private RemoteLogService remoteLogService;

    @Override
    public void record(OperLogEvent event) {
        OperLogDTO dto = new OperLogDTO();
        dto.setTenantId(event.getTenantId());
        dto.setModule(event.getModule());
        dto.setOperType(event.getOperType());
        dto.setMethod(event.getMethod());
        dto.setRequestMethod(event.getRequestMethod());
        dto.setOperUrl(event.getOperUrl());
        dto.setOperParam(event.getOperParam());
        dto.setOperUserId(event.getOperUserId());
        dto.setOperUsername(event.getOperUsername());
        dto.setOperIp(event.getOperIp());
        dto.setCostMs(event.getCostMs());
        dto.setStatus(event.getStatus());
        dto.setErrorMsg(event.getErrorMsg());
        dto.setOperTime(event.getOperTime());
        remoteLogService.recordOperLog(dto);
    }
}
