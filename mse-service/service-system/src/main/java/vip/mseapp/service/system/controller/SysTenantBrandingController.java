package vip.mseapp.service.system.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import vip.mseapp.core.constant.MseConstants;
import vip.mseapp.core.result.Result;
import vip.mseapp.service.system.entity.SysTenantBranding;
import vip.mseapp.service.system.service.BrandingAssetStorage;
import vip.mseapp.service.system.service.SysTenantBrandingService;
import vip.mseapp.service.system.entity.SysTenantBrandingDomain;
import vip.mseapp.service.system.service.SysTenantBrandingDomainService;
import vip.mseapp.web.log.OperLog;

import java.io.IOException;
import java.util.List;

/**
 * 租户品牌个性化（租户自助）：租户配置自己站点的登录页样式（sys_tenant_branding，每租户一行）
 * 与自助绑定站点域名（sys_tenant_branding_domain，域名全局唯一）。
 *
 * <p>平台自有登录页品牌走 service-platform 的 /tenant/domain-config（sys_domain_config），
 * 登录页解析只读 Redis 域名缓存（启动全量注册 + 变更写穿，DB 兜底）：租户绑定域名命中
 * 本租户品牌并锁定/隐藏租户编码，未命中回落平台自有配置。素材本地兜底读取
 * /system/tenant-branding/asset/{name} 免登录（img 标签不携带 token，微服务形态经网关
 * ignore-urls 放行；mse-fs 模式素材走 /fs/asset/** 不经此端点）。
 */
@Validated
@RestController
@RequestMapping("/system/tenant-branding")
public class SysTenantBrandingController {

    private final SysTenantBrandingService brandingService;
    private final SysTenantBrandingDomainService domainService;
    private final BrandingAssetStorage assetStorage;

    public SysTenantBrandingController(SysTenantBrandingService brandingService,
                                       SysTenantBrandingDomainService domainService,
                                       BrandingAssetStorage assetStorage) {
        this.brandingService = brandingService;
        this.domainService = domainService;
        this.assetStorage = assetStorage;
    }

    /** 当前租户的品牌配置（未配置过返回空白对象，前端直接表单编辑） */
    @SaCheckPermission(value = "system:branding:query", type = MseConstants.LOGIN_TYPE_TENANT)
    @GetMapping
    public Result<SysTenantBranding> get() {
        SysTenantBranding branding = brandingService.getOwn();
        return Result.ok(branding != null ? branding : new SysTenantBranding());
    }

    @OperLog(module = "system:branding", type = "修改")
    @SaCheckPermission(value = "system:branding:update", type = MseConstants.LOGIN_TYPE_TENANT)
    @PutMapping
    public Result<Void> save(@RequestBody @Validated BrandingSaveRequest request) {
        SysTenantBranding branding = new SysTenantBranding();
        branding.setSiteTitle(request.siteTitle());
        branding.setSiteLogo(request.siteLogo());
        branding.setLoginBackground(request.loginBackground());
        branding.setBackgroundColor(request.backgroundColor());
        branding.setCopyright(request.copyright());
        brandingService.saveOwn(branding);
        return Result.ok();
    }

    /** 当前租户的域名绑定列表（登录页解析按域名命中本租户品牌） */
    @SaCheckPermission(value = "system:branding:query", type = MseConstants.LOGIN_TYPE_TENANT)
    @GetMapping("/domain")
    public Result<List<SysTenantBrandingDomain>> listDomains() {
        return Result.ok(domainService.listOwn());
    }

    @OperLog(module = "system:branding", type = "绑定域名")
    @SaCheckPermission(value = "system:branding:update", type = MseConstants.LOGIN_TYPE_TENANT)
    @PostMapping("/domain")
    public Result<SysTenantBrandingDomain> bindDomain(@RequestBody @Validated DomainBindRequest request) {
        return Result.ok(domainService.bindOwn(request.domain()));
    }

    @OperLog(module = "system:branding", type = "解绑域名")
    @SaCheckPermission(value = "system:branding:update", type = MseConstants.LOGIN_TYPE_TENANT)
    @DeleteMapping("/domain/{id}")
    public Result<Void> unbindDomain(@PathVariable Long id) {
        domainService.unbindOwn(id);
        return Result.ok();
    }

    /** 品牌素材上传（kind = logo / background）；返回可直接写入 siteLogo / loginBackground 的站内地址 */
    @OperLog(module = "system:branding", type = "上传素材")
    @SaCheckPermission(value = "system:branding:update", type = MseConstants.LOGIN_TYPE_TENANT)
    @PostMapping("/asset")
    public Result<String> uploadAsset(@RequestParam("file") MultipartFile file,
                                      @RequestParam("kind") String kind) {
        return Result.ok(assetStorage.upload(file, kind));
    }

    /** 素材读取（免会话：登录页未登录也要加载 Logo/背景图；微服务形态经网关 ignore-urls 放行） */
    @GetMapping("/asset/{name}")
    public void asset(@PathVariable String name, HttpServletResponse response) throws IOException {
        assetStorage.write(name, response);
    }

    // -------------------- 请求体 --------------------

	/**
	 * 图片地址口径：http(s) 绝对地址、站内 / 路径或 data:image 内联，杜绝脚本类伪协议。
	 * 允许空串（留空 = 未配置；@Pattern 只放行 null 不放行 ""），落库前由服务层 blankToNull 归一。
	 */
	private static final String IMAGE_URL_PATTERN =
			"^(https?://.+|/.+|data:image/.+)?$";

    public record BrandingSaveRequest(
            @Size(max = 60, message = "标题最长 60 字符") String siteTitle,
            @Size(max = 500, message = "Logo 地址最长 500 字符")
            @Pattern(regexp = IMAGE_URL_PATTERN, message = "Logo 地址须为 http(s)://、/ 开头路径或 data:image 内联") String siteLogo,
            @Size(max = 500, message = "背景图地址最长 500 字符")
            @Pattern(regexp = IMAGE_URL_PATTERN, message = "背景图地址须为 http(s)://、/ 开头路径或 data:image 内联") String loginBackground,
            @Size(max = 200, message = "背景色最长 200 字符") String backgroundColor,
            @Size(max = 200, message = "版权文案最长 200 字符") String copyright) {
    }

    /** 域名格式：完整主机名（至少含一个点） */
    private static final String DOMAIN_PATTERN =
            "(?i)^[a-z0-9]([a-z0-9-]*[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)+$";

    public record DomainBindRequest(
            @NotBlank(message = "域名不能为空")
            @Size(max = 128, message = "域名最长 128 字符")
            @Pattern(regexp = DOMAIN_PATTERN, message = "域名格式不正确（完整主机名，如 demo.example.com）") String domain) {
    }
}
