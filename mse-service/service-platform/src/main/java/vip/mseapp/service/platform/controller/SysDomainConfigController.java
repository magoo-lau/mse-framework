package vip.mseapp.service.platform.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.servlet.http.HttpServletResponse;
import vip.mseapp.core.constant.MseConstants;
import vip.mseapp.core.result.PageResult;
import vip.mseapp.core.result.Result;
import vip.mseapp.service.platform.entity.SysDomainConfig;
import vip.mseapp.service.platform.service.DomainAssetStorage;
import vip.mseapp.service.platform.service.SysDomainConfigService;
import vip.mseapp.web.log.OperLog;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 域名个性化配置管理（平台运营端；sys_domain_config 为全局表）。
 *
 * <p>登录页公开解析接口在 service-auth 的 /auth/domain/config（经 RPC 调用本服务），
 * 品牌素材（Logo/背景图）读取 /tenant/domain-config/asset/{name} 免会话，
 * 本控制器其余端点仅承担平台侧维护。
 */
@Validated
@RestController
@RequestMapping("/tenant/domain-config")
public class SysDomainConfigController {

    private final SysDomainConfigService domainConfigService;
    private final DomainAssetStorage assetStorage;

    public SysDomainConfigController(SysDomainConfigService domainConfigService, DomainAssetStorage assetStorage) {
        this.domainConfigService = domainConfigService;
        this.assetStorage = assetStorage;
    }

    @SaCheckPermission(value = "tenant:domain:list", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @GetMapping("/page")
    public Result<PageResult<SysDomainConfig>> page(@RequestParam(defaultValue = "1") long pageNum,
                                                    @RequestParam(defaultValue = "10") long pageSize,
                                                    @RequestParam(required = false) String domain,
                                                    @RequestParam(required = false) Integer status) {
        return Result.ok(domainConfigService.pageDomainConfigs(pageNum, pageSize, domain, status));
    }

    @SaCheckPermission(value = "tenant:domain:query", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @GetMapping("/{id}")
    public Result<SysDomainConfig> get(@PathVariable Long id) {
        return Result.ok(domainConfigService.getById(id));
    }

    @OperLog(module = "tenant:domain", type = "新增")
    @SaCheckPermission(value = "tenant:domain:create", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PostMapping
    public Result<Long> create(@RequestBody @Validated DomainConfigSaveRequest request) {
        SysDomainConfig config = new SysDomainConfig();
        config.setDomain(request.domain());
        config.setSiteTitle(request.siteTitle());
        config.setSiteLogo(request.siteLogo());
        config.setLoginBackground(request.loginBackground());
        config.setBackgroundColor(request.backgroundColor());
        config.setCopyright(request.copyright());
        config.setRemark(request.remark());
        config.setStatus(request.status());
        return Result.ok(domainConfigService.createDomainConfig(config));
    }

    @OperLog(module = "tenant:domain", type = "修改")
    @SaCheckPermission(value = "tenant:domain:update", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @PutMapping
    public Result<Void> update(@RequestBody @Validated DomainConfigUpdateRequest request) {
        SysDomainConfig config = new SysDomainConfig();
        config.setId(request.id());
        config.setSiteTitle(request.siteTitle());
        config.setSiteLogo(request.siteLogo());
        config.setLoginBackground(request.loginBackground());
        config.setBackgroundColor(request.backgroundColor());
        config.setCopyright(request.copyright());
        config.setRemark(request.remark());
        config.setStatus(request.status());
        domainConfigService.updateDomainConfig(config);
        return Result.ok();
    }

    @OperLog(module = "tenant:domain", type = "删除")
    @SaCheckPermission(value = "tenant:domain:delete", type = MseConstants.LOGIN_TYPE_PLATFORM)
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        domainConfigService.deleteDomainConfig(id);
        return Result.ok();
    }

    /** 品牌素材上传（kind = logo / background）；返回可直接写入 siteLogo / loginBackground 的站内地址 */
    @OperLog(module = "tenant:domain", type = "上传素材")
    @SaCheckPermission(value = "tenant:domain:update", type = MseConstants.LOGIN_TYPE_PLATFORM)
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

    /** 域名格式：完整主机名（至少含一个点） */
    private static final String DOMAIN_PATTERN =
            "(?i)^[a-z0-9]([a-z0-9-]*[a-z0-9])?(\\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)+$";

    public record DomainConfigSaveRequest(
            @NotBlank(message = "域名不能为空")
            @Size(max = 128)
            @Pattern(regexp = DOMAIN_PATTERN, message = "域名格式不正确（完整主机名，如 demo.example.com）")
            String domain,
            @Size(max = 60, message = "标题最长 60 字符") String siteTitle,
            @Size(max = 500, message = "Logo 地址最长 500 字符")
            @Pattern(regexp = IMAGE_URL_PATTERN, message = "Logo 地址须为 http(s)://、/ 开头路径或 data:image 内联") String siteLogo,
            @Size(max = 500, message = "背景图地址最长 500 字符")
            @Pattern(regexp = IMAGE_URL_PATTERN, message = "背景图地址须为 http(s)://、/ 开头路径或 data:image 内联") String loginBackground,
            @Size(max = 200, message = "背景色最长 200 字符") String backgroundColor,
            @Size(max = 200, message = "版权文案最长 200 字符") String copyright,
            String remark,
            Integer status) {
    }

    public record DomainConfigUpdateRequest(
            @NotNull(message = "编号不能为空") Long id,
            @Size(max = 60, message = "标题最长 60 字符") String siteTitle,
            @Size(max = 500, message = "Logo 地址最长 500 字符")
            @Pattern(regexp = IMAGE_URL_PATTERN, message = "Logo 地址须为 http(s)://、/ 开头路径或 data:image 内联") String siteLogo,
            @Size(max = 500, message = "背景图地址最长 500 字符")
            @Pattern(regexp = IMAGE_URL_PATTERN, message = "背景图地址须为 http(s)://、/ 开头路径或 data:image 内联") String loginBackground,
            @Size(max = 200, message = "背景色最长 200 字符") String backgroundColor,
            @Size(max = 200, message = "版权文案最长 200 字符") String copyright,
            String remark,
            Integer status) {
    }
}
