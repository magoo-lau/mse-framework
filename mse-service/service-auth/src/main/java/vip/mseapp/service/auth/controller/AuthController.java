package vip.mseapp.service.auth.controller;

import cn.dev33.satoken.session.SaTerminalInfo;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import vip.mseapp.api.system.RemoteAuthService;
import vip.mseapp.api.system.RemoteAuthService.TotpEnrollment;
import vip.mseapp.api.system.RemoteUserService;
import vip.mseapp.api.system.dto.UserDTO;
import vip.mseapp.api.tenant.RemoteTenantService;
import vip.mseapp.api.tenant.dto.DomainConfigDTO;
import vip.mseapp.api.tenant.dto.TenantDTO;
import vip.mseapp.core.annotation.RpcReference;
import vip.mseapp.core.constant.MseConstants;
import vip.mseapp.core.exception.ServiceException;
import vip.mseapp.core.result.GlobalErrorCode;
import vip.mseapp.core.result.Result;
import vip.mseapp.security.crypto.MsePasswords;
import vip.mseapp.security.session.MseSessionSupport;
import vip.mseapp.security.stp.StpPlatformUtil;
import vip.mseapp.security.stp.StpTenantUtil;
import vip.mseapp.service.auth.support.ClientUa;
import vip.mseapp.service.auth.support.LoginCaptchaService;
import vip.mseapp.service.auth.support.LoginPolicyService;
import vip.mseapp.service.auth.support.LoginPolicyService.LoginPolicy;
import vip.mseapp.service.auth.support.LoginSessionManager;
import vip.mseapp.service.auth.support.LoginLogRecorder;
import vip.mseapp.service.auth.support.TwoFactorTokenService;
import vip.mseapp.service.auth.support.TwoFactorTokenService.PendingLogin;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 认证接口：租户端登录（tenantId/tenantCode + 账号密码）、平台端登录、域名解析、登出、当前身份、
 * 登录会话自助管理（多端会话列表/下线指定会话/下线其他设备）。
 *
 * <p><b>登录安全链</b>（开关经 sys_config「登录安全设置」即时生效，全部可关）：
 * ① 图形验证码——captchaMode=risk 时同账号/同 IP 连续失败达阈值自动要求（always=恒要求），登录主链路强制复核；
 * ② 双因子认证——密码通过后经临时凭证进入第二因子（TOTP 动态码 / 邮箱验证码），校验通过才签发正式 token；
 * totpMode=required 时未绑定用户先扫码绑定（绑定引导随临时凭证下发）。
 */
@Validated
@RestController
@RequestMapping("/auth")
public class AuthController {

    @RpcReference
    private RemoteUserService remoteUserService;

    @RpcReference
    private RemoteTenantService remoteTenantService;

    @RpcReference
    private RemoteAuthService remoteAuthService;

    private final LoginLogRecorder loginLogRecorder;
    private final LoginPolicyService loginPolicyService;
    private final LoginSessionManager loginSessionManager;
    private final LoginCaptchaService loginCaptchaService;
    private final TwoFactorTokenService twoFactorTokenService;

    public AuthController(LoginLogRecorder loginLogRecorder,
                          LoginPolicyService loginPolicyService,
                          LoginSessionManager loginSessionManager,
                          LoginCaptchaService loginCaptchaService,
                          TwoFactorTokenService twoFactorTokenService) {
        this.loginLogRecorder = loginLogRecorder;
        this.loginPolicyService = loginPolicyService;
        this.loginSessionManager = loginSessionManager;
        this.loginCaptchaService = loginCaptchaService;
        this.twoFactorTokenService = twoFactorTokenService;
    }

    /**
     * 登录页域名解析（公开接口）：按访问主机名定位绑定的租户（走域名缓存解析，
     * 租户自助绑定与控制面遗留单域名同一口径）。
     *
     * @param host 前端传入的访问主机名（location.hostname）
     * @return 命中返回租户编码/名称（登录页锁定编码）；未绑定任何租户返回 data=null
     */
    @GetMapping("/tenant/resolve")
    public Result<TenantResolveResult> resolveTenant(@RequestParam("host") String host) {
        String domain = normalizeHost(host);
        DomainConfigDTO config = domain == null ? null : remoteTenantService.getDomainConfig(domain);
        return Result.ok(config == null || config.getTenantId() == null ? null
                : new TenantResolveResult(config.getTenantId(), config.getTenantCode(), config.getTenantName()));
    }

    /**
     * 登录页个性化配置解析（公开接口）：按访问主机名返回域名定制的标题/Logo/背景/版权。
     *
     * @param host 前端传入的访问主机名（location.hostname）
     * @return 命中返回配置；未配置/已停用返回 data=null（登录页回退默认样式）
     */
    /**
     * 登录页个性化配置解析（公开接口，读 Redis 域名缓存/DB 兜底）：按访问主机名返回
     * 域名定制的标题/Logo/背景/版权；域名绑定租户时附带租户三要素（登录页据此
     * 锁定并隐藏租户编码），平台自有域名仅品牌字段。
     *
     * @param host 前端传入的访问主机名（location.hostname）
     * @return 命中返回配置；未配置返回 data=null（登录页回退默认样式）
     */
    @GetMapping("/domain/config")
    public Result<DomainConfigResult> domainConfig(@RequestParam("host") String host) {
        String domain = normalizeHost(host);
        DomainConfigDTO config = domain == null ? null : remoteTenantService.getDomainConfig(domain);
        return Result.ok(config == null ? null : new DomainConfigResult(
                config.getDomain(),
                config.getTenantId(),
                config.getTenantCode(),
                config.getTenantName(),
                config.getSiteTitle(),
                config.getSiteLogo(),
                config.getLoginBackground(),
                config.getBackgroundColor(),
                config.getCopyright()));
    }

    /**
     * 图形验证码预检（公开接口）：登录页加载/失败后调用，按当前风控态势返回是否需要验证码与图。
     * captchaMode=none 时恒返回 required=false；risk 模式下失败计数达标自动浮出（无需重启/改配置）。
     */
    @GetMapping("/captcha/new")
    public Result<LoginCaptchaService.CaptchaChallenge> captchaNew(
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) String tenantCode,
            @RequestParam(required = false) String username,
            HttpServletRequest httpRequest) {
        Long effectiveTenantId = resolveTenantIdForLog(tenantId, tenantCode);
        LoginPolicy policy = loginPolicyService.resolve(effectiveTenantId);
        return Result.ok(loginCaptchaService.challenge(policy, effectiveTenantId, username,
                ClientUa.resolveIp(httpRequest)));
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody @Validated TenantLoginRequest request, HttpServletRequest httpRequest) {
        try {
            // 1. 定位并校验租户
            TenantDTO tenant = resolveTenant(request.tenantId(), request.tenantCode());

            // 2. 域名绑定校验：来源域名已绑定租户时仅允许登录该租户（防绕过前端锁定）
            checkDomainBinding(request.originDomain(), tenant);

            // 3. 图形验证码（按当前风控判定，主链路强制复核防绕过登录页预检；失败只计 IP）
            LoginPolicy policy = loginPolicyService.resolve(tenant.getId());
            String ip = ClientUa.resolveIp(httpRequest);
            loginCaptchaService.verify(policy, tenant.getId(), request.username(), ip,
                    request.captchaId(), request.captchaCode());

            // 4. 查询用户（实现内部 runAs 指定租户语义）+ 密码校验（失败计入账号/IP 风控计数）
            UserDTO user = remoteUserService.getUserByUsername(tenant.getId(), request.username());
            if (user == null || !MsePasswords.matches(request.password(), user.getPassword())) {
                loginCaptchaService.recordPasswordFailure(tenant.getId(), request.username(), ip);
                throw new ServiceException(GlobalErrorCode.AUTH_LOGIN_FAILED);
            }
            if (Integer.valueOf(1).equals(user.getStatus())) {
                throw new ServiceException(GlobalErrorCode.AUTH_USER_DISABLED);
            }

            // 5. 双因子判定：required 全员进入；optional 仅绑定 TOTP 的用户进入
            boolean totpBound = Boolean.TRUE.equals(user.getTotpBound());
            if (policy.twoFactorRequired(totpBound)) {
                boolean enroll = policy.totpRequiredAll() && !totpBound;
                boolean emailAvailable = policy.emailOtp()
                        && remoteAuthService.isEmailChannelAvailable(tenant.getId(), user.getId());
                String tempToken = twoFactorTokenService.issue(new PendingLogin(
                        tenant.getId(), user.getId(), user.getUsername(), user.getNickname(),
                        false, request.device(), enroll, emailAvailable, maskEmail(user.getEmail())));
                loginLogRecorder.record(httpRequest, tenant.getId(), request.username(), true, "密码校验通过，等待双因子验证");
                return Result.ok(LoginResponse.twoFactor(enroll, tempToken));
            }

            // 6. 签发 token（loginId = tenantId:userId，天然携带租户归属）
            //    登录策略（单点/同端互斥/多端并发）按租户参数即时生效，被顶旧端收到差异化提示
            LoginResult result = issueToken(false, tenant.getId(), user, request.device(), policy, httpRequest);
            loginCaptchaService.resetFailures(tenant.getId(), request.username(), ip);
            loginLogRecorder.record(httpRequest, tenant.getId(), request.username(), true, "登录成功");
            return Result.ok(LoginResponse.ok(result));
        } catch (ServiceException ex) {
            loginLogRecorder.record(httpRequest, resolveTenantIdForLog(request.tenantId(), request.tenantCode()),
                    request.username(), false, ex.getMessage());
            throw ex;
        }
    }

    @PostMapping("/platform/login")
    public Result<LoginResponse> platformLogin(@RequestBody @Validated PlatformLoginRequest request,
                                               HttpServletRequest httpRequest) {
        try {
            // 平台管理员：租户 0 语义（验证码/双因子参数同样读平台域 tenant 0）
            Long tenantId = MseConstants.PLATFORM_TENANT_ID;
            LoginPolicy policy = loginPolicyService.resolve(tenantId);
            String ip = ClientUa.resolveIp(httpRequest);
            loginCaptchaService.verify(policy, tenantId, request.username(), ip,
                    request.captchaId(), request.captchaCode());

            UserDTO user = remoteUserService.getUserByUsername(tenantId, request.username());
            if (user == null || !MsePasswords.matches(request.password(), user.getPassword())) {
                loginCaptchaService.recordPasswordFailure(tenantId, request.username(), ip);
                throw new ServiceException(GlobalErrorCode.AUTH_LOGIN_FAILED);
            }
            if (Integer.valueOf(1).equals(user.getStatus())) {
                throw new ServiceException(GlobalErrorCode.AUTH_USER_DISABLED);
            }

            boolean totpBound = Boolean.TRUE.equals(user.getTotpBound());
            if (policy.twoFactorRequired(totpBound)) {
                boolean enroll = policy.totpRequiredAll() && !totpBound;
                boolean emailAvailable = policy.emailOtp()
                        && remoteAuthService.isEmailChannelAvailable(tenantId, user.getId());
                String tempToken = twoFactorTokenService.issue(new PendingLogin(
                        tenantId, user.getId(), user.getUsername(), user.getNickname(),
                        true, request.device(), enroll, emailAvailable, maskEmail(user.getEmail())));
                loginLogRecorder.record(httpRequest, tenantId, request.username(), true, "密码校验通过，等待双因子验证");
                return Result.ok(LoginResponse.twoFactor(enroll, tempToken));
            }

            LoginResult result = issueToken(true, tenantId, user, request.device(), policy, httpRequest);
            loginCaptchaService.resetFailures(tenantId, request.username(), ip);
            loginLogRecorder.record(httpRequest, tenantId, request.username(), true, "登录成功");
            return Result.ok(LoginResponse.ok(result));
        } catch (ServiceException ex) {
            loginLogRecorder.record(httpRequest, MseConstants.PLATFORM_TENANT_ID, request.username(), false, ex.getMessage());
            throw ex;
        }
    }

    /**
     * 双因子挑战信息（公开接口，凭临时凭证）：绑定引导返回二维码/密钥；已绑定返回通道可用性。
     * 邮箱通道可用性在密码校验通过时随临时凭证判定（开关 + SMTP + 用户邮箱）。
     */
    @GetMapping("/2fa/challenge")
    public Result<TwoFactorChallenge> twoFactorChallenge(@RequestParam("tempToken") String tempToken) {
        PendingLogin pending = twoFactorTokenService.require(tempToken);
        TotpEnrollment enrollment = pending.enroll()
                ? remoteAuthService.createPendingTotp(pending.tenantId(), pending.userId())
                : null;
        return Result.ok(new TwoFactorChallenge(
                pending.enroll(), enrollment, pending.emailAvailable(), pending.maskedEmail()));
    }

    /** 发送邮箱动态码（公开接口，凭临时凭证；冷却/限流/SMTP 检查在 service-system 侧） */
    @PostMapping("/2fa/email-code")
    public Result<String> sendEmailCode(@RequestBody @Validated TwoFactorTokenRequest request) {
        PendingLogin pending = twoFactorTokenService.require(request.tempToken());
        if (pending.enroll()) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "首次登录请先绑定验证器 App");
        }
        if (!pending.emailAvailable()) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "邮箱验证码通道不可用");
        }
        remoteAuthService.sendLoginEmailCode(pending.tenantId(), pending.userId());
        return Result.ok(pending.maskedEmail());
    }

    /**
     * 双因子验证换正式 token（公开接口，凭临时凭证）：TOTP（或绑定确认）二选一，
     * channel=email 走邮箱验证码。校验失败计入风控计数并限 5 次尝试（超限临时凭证作废）。
     */
    @PostMapping("/login/2fa")
    public Result<LoginResponse> twoFactorLogin(@RequestBody @Validated TwoFactorLoginRequest request,
                                                HttpServletRequest httpRequest) {
        PendingLogin pending = twoFactorTokenService.require(request.tempToken());
        String ip = ClientUa.resolveIp(httpRequest);
        boolean verified;
        if ("email".equalsIgnoreCase(request.channel())) {
            verified = remoteAuthService.verifyEmailCode(pending.tenantId(), pending.userId(), request.code());
        } else {
            verified = pending.enroll()
                    ? remoteAuthService.confirmPendingTotp(pending.tenantId(), pending.userId(), request.code())
                    : remoteAuthService.verifyTotp(pending.tenantId(), pending.userId(), request.code());
        }
        if (!verified) {
            twoFactorTokenService.recordFailure(request.tempToken());
            loginCaptchaService.recordPasswordFailure(pending.tenantId(), pending.username(), ip);
            loginLogRecorder.record(httpRequest, pending.tenantId(), pending.username(), false, "双因子验证码错误");
            throw new ServiceException(GlobalErrorCode.AUTH_LOGIN_FAILED.getCode(), "动态验证码错误或已过期");
        }
        twoFactorTokenService.consume(request.tempToken());

        LoginPolicy policy = loginPolicyService.resolve(pending.tenantId());
        LoginResult result = issueToken(pending.platform(), pending.tenantId(),
                pending.userId(), pending.username(), pending.nickname(), pending.device(), policy, httpRequest);
        loginCaptchaService.resetFailures(pending.tenantId(), pending.username(), ip);
        loginLogRecorder.record(httpRequest, pending.tenantId(), pending.username(), true, "登录成功");
        return Result.ok(LoginResponse.ok(result));
    }

    /** 登录失败日志的租户归属：已定位到租户用其编号，否则落 0（平台域，保证失败记录可查） */
    private Long resolveTenantIdForLog(Long tenantId, String tenantCode) {
        if (tenantId != null) {
            return tenantId;
        }
        if (tenantCode != null && !tenantCode.isBlank()) {
            TenantDTO tenant = remoteTenantService.getTenantByCode(tenantCode);
            if (tenant != null) {
                return tenant.getId();
            }
        }
        return MseConstants.PLATFORM_TENANT_ID;
    }

    private TenantDTO resolveTenant(Long tenantId, String tenantCode) {
        if (tenantId != null) {
            return remoteTenantService.validateTenant(tenantId);
        }
        if (tenantCode != null && !tenantCode.isBlank()) {
            TenantDTO tenant = remoteTenantService.getTenantByCode(tenantCode);
            if (tenant == null) {
                throw new ServiceException(GlobalErrorCode.TENANT_NOT_EXISTS);
            }
            // 与 validateTenant(id) 同口径：按编码解析的登录路径同样拒绝停用/过期租户（getTenantByCode
            // 是纯查询，失败日志路径 resolveTenantIdForLog 也依赖它，不能在 RPC 实现里抛）
            if (Integer.valueOf(1).equals(tenant.getStatus())) {
                throw new ServiceException(GlobalErrorCode.TENENT_DISABLED);
            }
            if (tenant.getExpireTime() != null && tenant.getExpireTime().isBefore(LocalDateTime.now())) {
                throw new ServiceException(GlobalErrorCode.TENANT_EXPIRED);
            }
            return tenant;
        }
        throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "tenantId 与 tenantCode 至少提供一项");
    }

    private void checkDomainBinding(String originDomain, TenantDTO tenant) {
        if (originDomain != null && !originDomain.isBlank()) {
            // 与登录页同一解析口径（域名缓存）：租户自助绑定同样参与域名-租户强制校验，防换码绕过
            DomainConfigDTO bound = remoteTenantService.getDomainConfig(normalizeHost(originDomain));
            if (bound != null && bound.getTenantId() != null && !bound.getTenantId().equals(tenant.getId())) {
                throw new ServiceException(GlobalErrorCode.AUTH_DOMAIN_TENANT_MISMATCH);
            }
        }
    }

    /** 签发正式 token（租户/平台双账号体系；登录策略顶号在此生效） */
    private LoginResult issueToken(boolean platform, Long tenantId, UserDTO user, String device,
                                   LoginPolicy policy, HttpServletRequest httpRequest) {
        return issueToken(platform, tenantId, user.getId(), user.getUsername(), user.getNickname(), device, policy, httpRequest);
    }

    private LoginResult issueToken(boolean platform, Long tenantId, Long userId, String username, String nickname,
                                   String device, LoginPolicy policy, HttpServletRequest httpRequest) {
        if (platform) {
            SaLoginParameter parameter = loginSessionManager.prepare(
                    StpPlatformUtil.stpLogic, userId, policy, device, httpRequest);
            StpPlatformUtil.login(userId, username, nickname, parameter);
            return new LoginResult(
                    MseConstants.TOKEN_NAME,
                    StpPlatformUtil.stpLogic.getTokenValue(),
                    MseConstants.PLATFORM_TENANT_ID,
                    userId,
                    nickname,
                    true);
        }
        SaLoginParameter parameter = loginSessionManager.prepare(
                StpTenantUtil.stpLogic, StpTenantUtil.buildLoginId(tenantId, userId), policy, device, httpRequest);
        StpTenantUtil.login(tenantId, userId, username, nickname, parameter);
        return new LoginResult(
                MseConstants.TOKEN_NAME,
                StpTenantUtil.stpLogic.getTokenValue(),
                tenantId,
                userId,
                nickname,
                false);
    }

    /** 邮箱脱敏（a***@example.com），供双因子阶段展示发送目标 */
    private static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return email;
        }
        return email.charAt(0) + "***" + email.substring(at);
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        if (StpTenantUtil.isLogin()) {
            StpTenantUtil.logout();
        } else if (StpPlatformUtil.isLogin()) {
            StpPlatformUtil.logout();
        }
        return Result.ok();
    }

    @GetMapping("/me")
    public Result<LoginResult> me() {
        if (StpTenantUtil.isLogin()) {
            return Result.ok(new LoginResult(
                    MseConstants.TOKEN_NAME,
                    StpTenantUtil.stpLogic.getTokenValue(),
                    StpTenantUtil.getTenantIdFromLoginId(),
                    StpTenantUtil.getUserIdFromLoginId(),
                    String.valueOf(StpTenantUtil.getSession().get(StpTenantUtil.SESSION_NICKNAME)),
                    false));
        }
        if (StpPlatformUtil.isLogin()) {
            return Result.ok(new LoginResult(
                    MseConstants.TOKEN_NAME,
                    StpPlatformUtil.stpLogic.getTokenValue(),
                    MseConstants.PLATFORM_TENANT_ID,
                    StpPlatformUtil.getUserId(),
                    String.valueOf(StpPlatformUtil.getSession().get(StpPlatformUtil.SESSION_NICKNAME)),
                    true));
        }
        throw new ServiceException(GlobalErrorCode.UNAUTHORIZED);
    }

    // -------------------- 登录会话自助管理（登录即可用，无需权限点） --------------------

    /**
     * 我的登录会话列表（当前登录侧的全部在线会话，含设备/IP/浏览器/登录时间；sessionId 为不可逆摘要）。
     */
    @GetMapping("/session/list")
    public Result<List<SessionItem>> sessionList() {
        if (StpTenantUtil.isLogin()) {
            return Result.ok(sessionItems(StpTenantUtil.stpLogic, StpTenantUtil.getLoginIdAsString()));
        }
        if (StpPlatformUtil.isLogin()) {
            return Result.ok(sessionItems(StpPlatformUtil.stpLogic, StpPlatformUtil.stpLogic.getLoginIdAsString()));
        }
        throw new ServiceException(GlobalErrorCode.UNAUTHORIZED);
    }

    /**
     * 下线我的一条登录会话（当前会话不可在此下线，走退出登录）。
     */
    @PostMapping("/session/logout")
    public Result<Void> sessionLogout(@RequestBody @Validated SessionLogoutRequest request) {
        if (StpTenantUtil.isLogin()) {
            kickOwnSession(StpTenantUtil.stpLogic, StpTenantUtil.getLoginIdAsString(), request.sessionId());
        } else if (StpPlatformUtil.isLogin()) {
            kickOwnSession(StpPlatformUtil.stpLogic, StpPlatformUtil.stpLogic.getLoginIdAsString(), request.sessionId());
        } else {
            throw new ServiceException(GlobalErrorCode.UNAUTHORIZED);
        }
        return Result.ok();
    }

    /**
     * 下线我的其他全部会话（保留当前会话）。
     */
    @PostMapping("/session/logout-others")
    public Result<Void> sessionLogoutOthers() {
        if (StpTenantUtil.isLogin()) {
            MseSessionSupport.kickOthers(StpTenantUtil.stpLogic, StpTenantUtil.getLoginIdAsString(),
                    StpTenantUtil.stpLogic.getTokenValue());
        } else if (StpPlatformUtil.isLogin()) {
            MseSessionSupport.kickOthers(StpPlatformUtil.stpLogic, StpPlatformUtil.stpLogic.getLoginIdAsString(),
                    StpPlatformUtil.stpLogic.getTokenValue());
        } else {
            throw new ServiceException(GlobalErrorCode.UNAUTHORIZED);
        }
        return Result.ok();
    }

    private List<SessionItem> sessionItems(StpLogic stpLogic, String loginId) {
        String currentToken = stpLogic.getTokenValue();
        List<SessionItem> items = new ArrayList<>();
        for (SaTerminalInfo terminal : stpLogic.getTerminalListByLoginId(loginId)) {
            String tokenValue = terminal.getTokenValue();
            items.add(new SessionItem(
                    MseSessionSupport.sessionId(tokenValue),
                    MseSessionSupport.deviceType(terminal),
                    MseSessionSupport.extra(terminal, MseSessionSupport.EXTRA_IP),
                    MseSessionSupport.extra(terminal, MseSessionSupport.EXTRA_BROWSER),
                    MseSessionSupport.extra(terminal, MseSessionSupport.EXTRA_OS),
                    toLocalDateTime(terminal.getCreateTime()),
                    tokenValue.equals(currentToken)));
        }
        // 当前会话置顶，其余按登录时间倒序
        items.sort(Comparator
                .comparing((SessionItem item) -> item.current() ? 0 : 1)
                .thenComparing(SessionItem::loginTime, Comparator.nullsLast(Comparator.reverseOrder())));
        return items;
    }

    private void kickOwnSession(StpLogic stpLogic, String loginId, String sessionId) {
        if (MseSessionSupport.sessionId(stpLogic.getTokenValue()).equals(sessionId)) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "不能下线当前会话，请使用退出登录");
        }
        if (!MseSessionSupport.kickBySessionId(stpLogic, loginId, sessionId)) {
            throw new ServiceException(GlobalErrorCode.BAD_REQUEST.getCode(), "会话不存在或已下线");
        }
    }

    private LocalDateTime toLocalDateTime(long epochMilli) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZoneId.systemDefault());
    }

    // -------------------- 请求/响应 --------------------

    public record TenantLoginRequest(
            Long tenantId,
            String tenantCode,
            @NotBlank(message = "用户名不能为空") @Size(max = 30) String username,
            @NotBlank(message = "密码不能为空") String password,
            /** 访问来源主机名（前端传 location.hostname）；域名已绑定租户时后端强制只允许登录该租户 */
            String originDomain,
            /** 登录设备类型（pc/app/mini 等，可空默认 pc）；同端互斥策略与在线会话展示依赖此值 */
            @Size(max = 20, message = "设备类型过长") String device,
            /** 图形验证码（风控要求时必传）：预检返回的 captchaId */
            String captchaId,
            /** 图形验证码用户输入 */
            String captchaCode) {
    }

    public record PlatformLoginRequest(
            @NotBlank(message = "用户名不能为空") @Size(max = 30) String username,
            @NotBlank(message = "密码不能为空") String password,
            /** 登录设备类型（可空默认 pc） */
            @Size(max = 20, message = "设备类型过长") String device,
            String captchaId,
            String captchaCode) {
    }

    public record LoginResult(String tokenName, String tokenValue, Long tenantId, Long userId,
                              String nickname, boolean platform) {
    }

    /**
     * 登录响应（两段式）：twoFactorRequired=false 时 result 即正式登录态；
     * true 时持有 tempToken 走 /auth/login/2fa（enroll=true 表示需先扫码绑定验证器）。
     */
    public record LoginResponse(boolean twoFactorRequired, boolean totpEnroll, String tempToken, LoginResult result) {

        public static LoginResponse ok(LoginResult result) {
            return new LoginResponse(false, false, null, result);
        }

        public static LoginResponse twoFactor(boolean enroll, String tempToken) {
            return new LoginResponse(true, enroll, tempToken, null);
        }
    }

    /** 双因子挑战：enroll=true 附绑定挑战（二维码 + 手输密钥）；否则附邮箱通道可用性与脱敏邮箱 */
    public record TwoFactorChallenge(boolean enroll, TotpEnrollment enrollment,
                                     boolean emailAvailable, String maskedEmail) {
    }

    public record TwoFactorTokenRequest(@NotBlank(message = "临时凭证不能为空") String tempToken) {
    }

    public record TwoFactorLoginRequest(
            @NotBlank(message = "临时凭证不能为空") String tempToken,
            @NotBlank(message = "动态验证码不能为空") @Size(max = 10, message = "验证码过长") String code,
            /** 第二因子通道：totp（默认）/ email */
            String channel) {
    }

    /** 我的登录会话条目（sessionId 为 SHA-256 摘要前 16 位，非原始 token） */
    public record SessionItem(String sessionId, String deviceType, String ip, String browser,
                              String os, LocalDateTime loginTime, boolean current) {
    }

    public record SessionLogoutRequest(
            @NotBlank(message = "会话标识不能为空") String sessionId) {
    }

    /** 域名解析结果：登录页据此回填并锁定租户编码 */
    public record TenantResolveResult(Long tenantId, String tenantCode, String tenantName) {
    }

    /**
     * 域名个性化配置：登录页据此渲染标题/Logo/背景/版权；tenantId 非空 = 域名绑定租户，
     * 登录页以 tenantCode 锁定并隐藏租户编码输入。
     */
    public record DomainConfigResult(String domain, Long tenantId, String tenantCode, String tenantName,
                                     String siteTitle, String siteLogo, String loginBackground,
                                     String backgroundColor, String copyright) {
    }

    /** 主机名规范化：trim + 小写 + 剥离可能携带的端口；空白返回 null */
    private String normalizeHost(String host) {
        if (host == null || host.isBlank()) {
            return null;
        }
        String value = host.trim().toLowerCase();
        int portIdx = value.indexOf(':');
        if (portIdx > 0 && !value.startsWith("[")) {
            value = value.substring(0, portIdx);
        }
        return value.isBlank() ? null : value;
    }
}
