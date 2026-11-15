package vip.mseapp.service.system.service;

import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import vip.mseapp.core.exception.ServiceException;
import vip.mseapp.core.result.GlobalErrorCode;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 品牌素材上传规格（各存储实现共用）：素材类型 / 大小上限 / 扩展名白名单校验，
 * 扩展名 → 固定 Content-Type 定型（不信任上传方声明的类型）。
 */
final class BrandingAssetRules {

    /** 历史本地文件名口径（防路径穿越：只允许数字 id + 白名单扩展名） */
    static final Pattern ASSET_NAME_PATTERN =
            Pattern.compile("^\\d+\\.(png|jpg|jpeg|webp|gif|svg|ico)$");

    /** 扩展名 → 固定 Content-Type */
    static final Map<String, String> CONTENT_TYPES = Map.of(
            "png", "image/png",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "webp", "image/webp",
            "gif", "image/gif",
            "svg", "image/svg+xml",
            "ico", "image/x-icon"
    );

    private static final long MAX_LOGO_SIZE = 2L * 1024 * 1024;
    private static final long MAX_BACKGROUND_SIZE = 8L * 1024 * 1024;

    private static final Set<String> LOGO_EXTS = Set.of("png", "jpg", "jpeg", "webp", "gif", "svg", "ico");
    private static final Set<String> BACKGROUND_EXTS = Set.of("png", "jpg", "jpeg", "webp");

    private BrandingAssetRules() {
    }

    /** 校验 kind / 大小 / 扩展名白名单，返回小写扩展名（非法即抛 400 业务异常——请求侧可修正问题，
     * 不用 ServiceException(String) 缺省 500 码） */
    static String validate(MultipartFile file, String kind) {
        boolean isLogo = "logo".equals(kind);
        if (!isLogo && !"background".equals(kind)) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "素材类型仅支持 logo / background");
        }
        if (file == null || file.isEmpty()) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "上传文件为空");
        }
        long maxSize = isLogo ? MAX_LOGO_SIZE : MAX_BACKGROUND_SIZE;
        String limitText = isLogo ? "Logo 不能超过 2MB" : "背景图不能超过 8MB";
        if (file.getSize() > maxSize) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), limitText);
        }
        String ext = extension(file.getOriginalFilename());
        if (!CONTENT_TYPES.containsKey(ext)
                || !(isLogo ? LOGO_EXTS : BACKGROUND_EXTS).contains(ext)) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), isLogo
                    ? "Logo 仅支持 png / jpg / webp / gif / svg / ico"
                    : "背景图仅支持 png / jpg / webp");
        }
        return ext;
    }

    static String extension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
