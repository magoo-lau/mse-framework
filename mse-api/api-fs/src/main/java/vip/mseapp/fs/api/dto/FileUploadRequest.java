package vip.mseapp.fs.api.dto;

import java.io.Serializable;

/**
 * 文件上传请求（内容 base64 编码；RPC 链路适合小文件，大文件走 HTTP 端点）。
 */
public class FileUploadRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 原始文件名（必填，含扩展名） */
    private String fileName;

    /** 内容 MIME 类型（可选，默认 application/octet-stream） */
    private String contentType;

    /** 文件内容（Base64 编码，必填） */
    private String contentBase64;

    /** 业务分类（可选，如 oa/attachment、ai/document，便于按业务检索） */
    private String bizType;

    /** 业务关联 ID（可选，如表单实例 ID） */
    private String bizId;

    /** 访问级别（可选：vip.mseapp.fs.api.FileAccessLevel.TENANT 默认 / PUBLIC 公开可匿名读） */
    private String accessLevel;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getContentBase64() {
        return contentBase64;
    }

    public void setContentBase64(String contentBase64) {
        this.contentBase64 = contentBase64;
    }

    public String getBizType() {
        return bizType;
    }

    public void setBizType(String bizType) {
        this.bizType = bizType;
    }

    public String getBizId() {
        return bizId;
    }

    public void setBizId(String bizId) {
        this.bizId = bizId;
    }

    public String getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(String accessLevel) {
        this.accessLevel = accessLevel;
    }
}
