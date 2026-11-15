package vip.mseapp.api.system.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志事件（内部 RPC 契约，字段与 common-web 的 OperLogEvent 对应）。
 */
public class OperLogDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long tenantId;
    /** 业务模块（注解 module） */
    private String module;
    /** 操作类型（注解 type） */
    private String operType;
    /** 方法名（类.方法） */
    private String method;
    /** 请求方式（GET/POST/PUT/DELETE） */
    private String requestMethod;
    /** 请求 URL */
    private String operUrl;
    /** 请求参数（JSON，超长截断） */
    private String operParam;
    private Long operUserId;
    private String operUsername;
    private String operIp;
    /** 耗时（毫秒） */
    private Integer costMs;
    /** 0 成功 1 失败 */
    private Integer status;
    /** 错误消息（超长截断） */
    private String errorMsg;
    private LocalDateTime operTime;

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getOperType() {
        return operType;
    }

    public void setOperType(String operType) {
        this.operType = operType;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public String getOperUrl() {
        return operUrl;
    }

    public void setOperUrl(String operUrl) {
        this.operUrl = operUrl;
    }

    public String getOperParam() {
        return operParam;
    }

    public void setOperParam(String operParam) {
        this.operParam = operParam;
    }

    public Long getOperUserId() {
        return operUserId;
    }

    public void setOperUserId(Long operUserId) {
        this.operUserId = operUserId;
    }

    public String getOperUsername() {
        return operUsername;
    }

    public void setOperUsername(String operUsername) {
        this.operUsername = operUsername;
    }

    public String getOperIp() {
        return operIp;
    }

    public void setOperIp(String operIp) {
        this.operIp = operIp;
    }

    public Integer getCostMs() {
        return costMs;
    }

    public void setCostMs(Integer costMs) {
        this.costMs = costMs;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public LocalDateTime getOperTime() {
        return operTime;
    }

    public void setOperTime(LocalDateTime operTime) {
        this.operTime = operTime;
    }
}
