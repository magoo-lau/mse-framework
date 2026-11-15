package vip.mseapp.fs.api;

/**
 * 文件访问级别常量（fs_file.access_level 列取值；经 FileUploadRequest.accessLevel 声明）。
 */
public final class FileAccessLevel {

    /** 租户私有（默认）：元数据租户插件隔离，下载需登录且限定本租户 */
    public static final String TENANT = "tenant";

    /** 公开：可经 mse-fs 匿名端点 /fs/asset/{tenantId}/{fileId}.{后缀} 读取（登录页素材等免登录场景专用） */
    public static final String PUBLIC = "public";

    private FileAccessLevel() {
    }
}
