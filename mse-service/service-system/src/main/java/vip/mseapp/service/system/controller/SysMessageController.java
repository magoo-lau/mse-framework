package vip.mseapp.service.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckOr;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vip.mseapp.api.system.dto.MessageDTO;
import vip.mseapp.core.constant.MseConstants;
import vip.mseapp.core.result.PageResult;
import vip.mseapp.core.result.Result;
import vip.mseapp.service.system.service.SysMessageService;

/**
 * 消息中心（双端工作台通用，接收人个人数据：登录即可读写自己的消息；
 * 投递不经 HTTP，走 RemoteMessageService 契约）。
 */
@Validated
@RestController
@RequestMapping("/system/message")
public class SysMessageController {

    private final SysMessageService messageService;

    public SysMessageController(SysMessageService messageService) {
        this.messageService = messageService;
    }

    @SaCheckOr(login = {
            @SaCheckLogin(type = MseConstants.LOGIN_TYPE_TENANT),
            @SaCheckLogin(type = MseConstants.LOGIN_TYPE_PLATFORM)
    })
    @GetMapping("/page")
    public Result<PageResult<MessageDTO>> page(@RequestParam(defaultValue = "1") long pageNum,
                                               @RequestParam(defaultValue = "10") long pageSize,
                                               @RequestParam(required = false) String type,
                                               @RequestParam(required = false) Boolean unreadOnly) {
        return Result.ok(messageService.pageMessages(pageNum, pageSize, type, unreadOnly));
    }

    @SaCheckOr(login = {
            @SaCheckLogin(type = MseConstants.LOGIN_TYPE_TENANT),
            @SaCheckLogin(type = MseConstants.LOGIN_TYPE_PLATFORM)
    })
    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.ok(messageService.unreadCount());
    }

    @SaCheckOr(login = {
            @SaCheckLogin(type = MseConstants.LOGIN_TYPE_TENANT),
            @SaCheckLogin(type = MseConstants.LOGIN_TYPE_PLATFORM)
    })
    @PostMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        messageService.markRead(id);
        return Result.ok();
    }

    @SaCheckOr(login = {
            @SaCheckLogin(type = MseConstants.LOGIN_TYPE_TENANT),
            @SaCheckLogin(type = MseConstants.LOGIN_TYPE_PLATFORM)
    })
    @PostMapping("/read-all")
    public Result<Void> markAllRead() {
        messageService.markAllRead();
        return Result.ok();
    }
}
