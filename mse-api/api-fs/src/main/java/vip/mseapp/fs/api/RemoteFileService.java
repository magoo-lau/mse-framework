package vip.mseapp.fs.api;

/**
 * 文件存储契约（mse-modules/mse-fs 的 fs-service 实现；单体聚合 fs-service 时本地直调，微服务形态经 RPC 远调）。
 *
 * <p>机器语义约定：请求/返回均为 JSON 字符串，租户上下文由调用方注入、经 RPC 隐式透传
 * （文件归属租户 = 调用方上下文租户，元数据与物理对象双层隔离）。
 * RPC 链路经 base64 携带内容，适合小文件；大文件请走 mse-fs 的 HTTP 上传端点。
 */
public interface RemoteFileService {

    /**
     * 上传文件（内容 base64 编码）。
     *
     * @param requestJson FileUploadRequest JSON：fileName（必填）、contentType、contentBase64（必填）、
     *                    bizType、bizId（均可选）、accessLevel（可选，tenant 默认 / public 公开可匿名读）
     * @return FileMetaResult JSON：fileId、fileName、contentType、fileSize、storageType、bizType、bizId、accessLevel、createTime
     */
    String uploadFile(String requestJson);

    /**
     * 查询文件元数据。
     *
     * @param requestJson FileMetaRequest JSON：fileId（必填）
     * @return FileMetaResult JSON（跨租户 fileId 查不到时抛 404 语义错误）
     */
    String getFileMeta(String requestJson);

    /**
     * 下载文件内容（base64 编码回传；仅限调用方上下文租户内的文件）。
     *
     * @param requestJson FileMetaRequest JSON：fileId（必填）
     * @return FileDownloadResult JSON：fileId、fileName、contentType、fileSize、contentBase64
     */
    String downloadFile(String requestJson);

    /**
     * 删除文件（逻辑删元数据 + 尽力删除物理对象）。
     *
     * @param requestJson FileMetaRequest JSON：fileId（必填）
     * @return FileOperationResult JSON：fileId、deleted
     */
    String deleteFile(String requestJson);
}
