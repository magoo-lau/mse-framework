package vip.mseapp.rpc.dubbo;

import vip.mseapp.core.constant.MseConstants;
import vip.mseapp.core.context.TenantContext;
import vip.mseapp.core.context.UserContext;
import vip.mseapp.core.log.LogContext;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
import org.slf4j.MDC;

/**
 * Dubbo 租户/用户上下文透传过滤器（SPI 自动激活，consumer + provider 双端）。
 *
 * <p>consumer：把 TenantContext/UserContext 与 MDC 中的 traceId 写入 RPC attachment；
 * provider：从 attachment 恢复上下文并镜像进日志上下文（MDC），出口清理（Dubbo 线程池复用，防止串号）。
 */
@Activate(group = {CommonConstants.CONSUMER, CommonConstants.PROVIDER}, order = Integer.MIN_VALUE + 1000)
public class MseDubboContextFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        if (RpcContext.getServiceContext().isConsumerSide()) {
            Long tenantId = TenantContext.getTenantId();
            if (tenantId != null) {
                RpcContext.getClientAttachment().setAttachment(MseConstants.RPC_TENANT_KEY, String.valueOf(tenantId));
            }
            Long userId = UserContext.getUserId();
            if (userId != null) {
                RpcContext.getClientAttachment().setAttachment(MseConstants.RPC_USER_KEY, String.valueOf(userId));
            }
            String username = UserContext.getUsername();
            if (username != null) {
                RpcContext.getClientAttachment().setAttachment(MseConstants.RPC_USERNAME_KEY, username);
            }
            if (UserContext.isPlatform()) {
                // 平台会话标记必须显式透传：provider 端 UserInfo.platform 无法从 tenant 0 反推
                // （UserContext 与 TenantContext 是两个通道），漏传会把平台调用误判为租户端
                RpcContext.getClientAttachment().setAttachment(MseConstants.RPC_PLATFORM_KEY, "1");
            }
            String traceId = MDC.get(LogContext.TRACE_ID);
            if (traceId != null && !traceId.isBlank()) {
                RpcContext.getClientAttachment().setAttachment(MseConstants.RPC_TRACE_KEY, traceId);
            }
            return invoker.invoke(invocation);
        }

        // provider 端：恢复上下文并镜像日志上下文，出口清理
        String tenantId = invocation.getAttachment(MseConstants.RPC_TENANT_KEY);
        if (tenantId != null && !tenantId.isBlank()) {
            TenantContext.setTenantId(Long.valueOf(tenantId));
        }
        String userId = invocation.getAttachment(MseConstants.RPC_USER_KEY);
        String username = invocation.getAttachment(MseConstants.RPC_USERNAME_KEY);
        boolean platform = "1".equals(invocation.getAttachment(MseConstants.RPC_PLATFORM_KEY));
        if (userId != null) {
            UserContext.set(new UserContext.UserInfo(Long.valueOf(userId), username, platform));
        }
        String traceId = invocation.getAttachment(MseConstants.RPC_TRACE_KEY);
        if (traceId != null && !traceId.isBlank()) {
            LogContext.setTraceId(traceId);
        }
        try {
            LogContext.syncContext();
            return invoker.invoke(invocation);
        } finally {
            TenantContext.clear();
            UserContext.clear();
            LogContext.clear();
        }
    }
}
