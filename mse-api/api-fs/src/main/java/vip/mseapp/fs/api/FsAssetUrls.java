package vip.mseapp.fs.api;

import java.util.Locale;

/**
 * mse-fs 公开素材 URL 契约（唯一合法拼法，格式变更只改此处）。
 *
 * <p>格式固定为 {@code /fs/asset/{tenantId}/{fileId}.{后缀}}：首段租户号即素材所属世界
 * （0=平台世界，其余为租户编号）——免登录端点按该段做语句级路由（runAs），
 * 精确复用上传时的落库决策：平台世界在双库形态落平台库、微服务形态落业务主库，
 * 租户世界落共享主库（tenant_id 行过滤）或租户独立库；租户段与行 tenant_id 不符即 404。
 * 尾段真实后缀仅为可读性（无后缀的地址同样可访问），取自上传时的原始文件名，
 * 净化规则与 fs-service FileStorage.sanitizeExt 同口径（小写字母数字 1-16，无效省略）。
 *
 * <p>上传方在会话内调用 {@link #publicAssetUrl(long, long, String)}（tenantId 取自当前上下文，
 * fileName 取上传原始文件名），返回值可直接写入 img 标签 / 落库；网关已放行 {@code /fs/asset/**}，
 * 前端经 /api 代理加载（mse-ui utils/asset.ts 的 /fs/ 前缀口径）。
 */
public final class FsAssetUrls {

    /** 免登录素材端点路径前缀（mse-fs FsPublicAssetController） */
    public static final String PUBLIC_ASSET_PREFIX = "/fs/asset/";

    private FsAssetUrls() {
    }

    /** 构造公开素材站内地址：{@code /fs/asset/{tenantId}/{fileId}.{后缀}}（无有效后缀时省略点段） */
    public static String publicAssetUrl(long tenantId, long fileId, String fileName) {
        String ext = extOf(fileName);
        return publicAssetUrl(tenantId, fileId) + (ext.isEmpty() ? "" : "." + ext);
    }

    /** 无后缀版本（fileId 内容不变可永久强缓存；端点对带/不带后缀同语义） */
    public static String publicAssetUrl(long tenantId, long fileId) {
        return PUBLIC_ASSET_PREFIX + tenantId + "/" + fileId;
    }

    /** 从原始文件名提取净化后缀（与 FileStorage.sanitizeExt 同口径），无效返回空串 */
    private static String extOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        String ext = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        return ext.matches("[a-z0-9]{1,16}") ? ext : "";
    }
}
