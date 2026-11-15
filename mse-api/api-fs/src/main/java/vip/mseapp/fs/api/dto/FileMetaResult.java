package vip.mseapp.fs.api.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 文件元数据返回。
 *
 * <p>本契约的载荷是 JSON 字符串（{@link RemoteFileService} 方法签名 String→String），
 * 日期格式必须在契约上钉死：服务端（容器 JsonMapper，全局定制 yyyy-MM-dd HH:mm:ss）
 * 与消费端（各自 mapper，可能是裸 mapper）才能对齐，否则裸 mapper 按 ISO（'T' 分隔）
 * 反序列化会在空格处解析失败。
 */
public class FileMetaResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long fileId;

    /** 原始文件名 */
    private String fileName;

    /** 内容 MIME 类型 */
    private String contentType;

    /** 字节数 */
    private Long fileSize;

    /** 存储类型：local / rustfs */
    private String storageType;

    /** 业务分类 */
    private String bizType;

    /** 业务关联 ID */
    private String bizId;

    /** 访问级别：tenant / public */
    private String accessLevel;

    /** 内容 MD5（32 位小写 hex；同租户内同 MD5 重复上传秒传复用物理对象） */
    private String contentMd5;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

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

    public String getStorageType() {
        return storageType;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
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

    public String getContentMd5() {
        return contentMd5;
    }

    public void setContentMd5(String contentMd5) {
        this.contentMd5 = contentMd5;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}
