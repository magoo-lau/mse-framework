package vip.mseapp.fs.api.dto;

import java.io.Serializable;

/**
 * 文件下载返回（内容 base64 编码；RPC 链路适合小文件，大文件请走 mse-fs 的 HTTP 下载端点）。
 */
public class FileDownloadResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 文件编号 */
    private Long fileId;

    /** 原始文件名（下载方设置 Content-Disposition 用） */
    private String fileName;

    /** 内容 MIME 类型（为空时按 application/octet-stream 处理） */
    private String contentType;

    /** 字节数（解码后的原始大小） */
    private Long fileSize;

    /** 文件内容（Base64 编码） */
    private String contentBase64;

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

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

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getContentBase64() {
        return contentBase64;
    }

    public void setContentBase64(String contentBase64) {
        this.contentBase64 = contentBase64;
    }
}
