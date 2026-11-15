package vip.mseapp.service.system.service;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 租户品牌素材（Logo/登录背景图）存储契约。
 *
 * <p>默认实现二选一，由装配按 mse-fs 可用性自动选择（{@code mse.tenant.branding-storage} 可显式覆盖）：
 * <ul>
 *   <li>{@link FsBrandingAssetStorage}：统一存 mse-fs 公开素材通道（access_level=public），
 *       返回 {@code /fs/asset/{tenantId}/{fileId}.{后缀}}；mse-fs 未部署/未引入时自动让位；</li>
 *   <li>{@link LocalBrandingAssetStorage}：本地磁盘兜底（框架区不依赖 mse-fs 部署也能跑），
 *       经 /system/tenant-branding/asset/{name} 免登录读取（微服务形态需加入网关 mse.gateway.ignore-urls）。</li>
 * </ul>
 * 声明同类型 Bean 即可整体替换（如 OSS/MinIO，{@code @ConditionalOnMissingBean(BrandingAssetStorage.class)}）。
 */
public interface BrandingAssetStorage {

    /**
     * 保存上传的品牌素材（kind = logo / background），返回可写入
     * siteLogo / loginBackground 的站内地址（登录页免登录可加载）。
     */
    String upload(MultipartFile file, String kind);

    /** 按文件名输出素材（inline + 永久强缓存 + nosniff；CSP 禁脚本防 SVG 直开执行） */
    void write(String name, HttpServletResponse response) throws IOException;
}
