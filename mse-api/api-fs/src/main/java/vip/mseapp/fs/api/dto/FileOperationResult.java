package vip.mseapp.fs.api.dto;

import java.io.Serializable;

/**
 * 文件删除操作返回。
 */
public class FileOperationResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long fileId;

    /** 是否删除成功（元数据已逻辑删；物理对象删除失败时仍为 true，孤儿对象可由存储侧清理） */
    private boolean deleted;

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
