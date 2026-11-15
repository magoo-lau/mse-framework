package vip.mseapp.fs.api.dto;

import java.io.Serializable;

/**
 * 文件元数据查询/下载/删除请求。
 */
public class FileMetaRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 文件编号（必填） */
    private Long fileId;

    public Long getFileId() {
        return fileId;
    }

    public void setFileId(Long fileId) {
        this.fileId = fileId;
    }
}
