package vip.mseapp.service.platform.service;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.json.JsonMapper;
import vip.mseapp.core.annotation.RpcReference;
import vip.mseapp.core.context.TenantContext;
import vip.mseapp.core.exception.ServiceException;
import vip.mseapp.fs.api.FsAssetUrls;
import vip.mseapp.fs.api.FileAccessLevel;
import vip.mseapp.fs.api.RemoteFileService;
import vip.mseapp.fs.api.dto.FileMetaResult;
import vip.mseapp.fs.api.dto.FileUploadRequest;

import java.io.IOException;
import java.util.Base64;

/**
 * 品牌素材 mse-fs 公开素材通道存储（FsAvailable 时优先启用）。
 *
 * <p>上传走 {@link RemoteFileService#uploadFile}（accessLevel=public，bizType=tenant-branding），
 * 返回 {@code /fs/asset/{tenantId}/{fileId}.{后缀}}（租户段=平台世界 0）——mse-fs 的免登录素材端点
 * 按租户段路由落库寻址，fileId 内容不变可永久强缓存；
 * 微服务形态经 dubbo 远调 mse-fs，单体聚合 fs-service 时本地直调（mse-fs 未引入时本类不会被装配）。
 * 历史本地素材（配置行里仍指向 {@code /tenant/domain-config/asset/{name}} 的旧 URL）
 * 委托同名本地目录读取，保证切到 mse-fs 后旧素材不丢。
 */
public class FsDomainAssetStorage implements DomainAssetStorage {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    /** RPC 引用（60s：背景图 8MB base64 约 10.7MB，弱链路上传留足余量） */
    @RpcReference(dubbo = @RpcReference.Dubbo(timeout = 60_000))
    private RemoteFileService remoteFileService;

    /** 历史本地素材所在目录（与 LocalDomainAssetStorage 同键，保证切换后旧文件仍可读） */
    @Value("${mse.tenant.branding-dir:./branding-assets}")
    private String brandingDir;

    @Override
    public String upload(MultipartFile file, String kind) {
        String ext = DomainAssetRules.validate(file, kind);
        FileUploadRequest request = new FileUploadRequest();
        request.setFileName(file.getOriginalFilename());
        // 按扩展名定型 Content-Type，与本地实现同口径（不信任上传方声明）
        request.setContentType(DomainAssetRules.CONTENT_TYPES.get(ext));
        try {
            request.setContentBase64(Base64.getEncoder().encodeToString(file.getBytes()));
        } catch (IOException e) {
            throw new ServiceException("素材读取失败：" + e.getMessage());
        }
        request.setBizType("tenant-branding");
        request.setBizId(kind);
        request.setAccessLevel(FileAccessLevel.PUBLIC);
        FileMetaResult meta;
        try {
            meta = JSON.readValue(remoteFileService.uploadFile(JSON.writeValueAsString(request)),
                    FileMetaResult.class);
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("素材上传失败：" + e.getMessage());
        }
        return FsAssetUrls.publicAssetUrl(TenantContext.getRequiredTenantId(), meta.getFileId(), meta.getFileName());
    }

    @Override
    public void write(String name, HttpServletResponse response) throws IOException {
        // 本类返回的 /fs/asset/{tenantId}/{id}.{ext} 不经此端点；能走到这里的都是历史本地素材，按原名读旧目录
        legacy().write(name, response);
    }

    private LocalDomainAssetStorage legacy() {
        return new LocalDomainAssetStorage(brandingDir);
    }
}
