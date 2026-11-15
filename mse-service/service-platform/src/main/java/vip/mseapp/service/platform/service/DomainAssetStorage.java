package vip.mseapp.service.platform.service;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 域名个性化品牌素材存储契约（登录页 Logo / 背景图）。
 *
 * <p>默认实现二选一，由装配按 mse-fs 可用性自动选择（{@code mse.tenant.branding-storage} 可显式覆盖）：
 * <ul>
 *   <li>{@link FsDomainAssetStorage}：统一存 mse-fs 公开素材通道（access_level=public），
 *       返回 {@code /fs/asset/{tenantId}/{fileId}.{后缀}}；mse-fs 未部署/未引入时自动让位；</li>
 *   <li>{@link LocalDomainAssetStorage}：本地磁盘兜底（框架区不依赖 mse-fs 部署也能跑）。</li>
 * </ul>
 * 声明同类型 Bean 即可整体替换（如 OSS/MinIO，{@code @ConditionalOnMissingBean(DomainAssetStorage.class)}）。
 */
public interface DomainAssetStorage {

    /**
     * 保存上传的素材，返回可写入 siteLogo / loginBackground 的站内地址（免登录可加载）。
     *
     * @param kind logo / background（分别对应两类大小与扩展名白名单）
     */
    String upload(MultipartFile file, String kind);

    /** 按文件名输出素材（inline + 永久强缓存 + nosniff；CSP 禁脚本防 SVG 直开执行） */
    void write(String name, HttpServletResponse response) throws IOException;
}
