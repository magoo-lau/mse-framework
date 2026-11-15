package vip.mseapp.service.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;
import vip.mseapp.api.system.dto.MessageDTO;
import vip.mseapp.core.context.UserContext;
import vip.mseapp.core.result.PageResult;
import vip.mseapp.service.system.entity.SysMessage;
import vip.mseapp.service.system.mapper.SysMessageMapper;
import vip.mseapp.service.system.service.SysMessageService;
import org.springframework.util.StringUtils;

import java.util.Collection;

public class SysMessageServiceImpl extends CrudRepository<SysMessageMapper, SysMessage> implements SysMessageService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void send(Collection<Long> receiverIds, String type, String title, String content, String bizId) {
        if (receiverIds == null || receiverIds.isEmpty()) {
            return;
        }
        for (Long receiverId : receiverIds.stream().distinct().toList()) {
            if (receiverId == null) {
                continue;
            }
            SysMessage message = new SysMessage();
            message.setType(type);
            message.setReceiverId(receiverId);
            message.setTitle(title);
            message.setContent(content);
            message.setBizId(bizId);
            message.setReadFlag(0);
            save(message);
        }
    }

    @Override
    public PageResult<MessageDTO> pageMessages(long pageNum, long pageSize, String type, Boolean unreadOnly) {
        Page<SysMessage> page = page(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<SysMessage>()
                        .eq(SysMessage::getReceiverId, UserContext.getUserId())
                        .eq(StringUtils.hasText(type), SysMessage::getType, type)
                        .eq(Boolean.TRUE.equals(unreadOnly), SysMessage::getReadFlag, 0)
                        .orderByDesc(SysMessage::getId));
        return PageResult.of(page.getTotal(), pageNum, pageSize,
                page.getRecords().stream().map(SysMessageServiceImpl::toDto).toList());
    }

    @Override
    public long unreadCount() {
        return count(new LambdaQueryWrapper<SysMessage>()
                .eq(SysMessage::getReceiverId, UserContext.getUserId())
                .eq(SysMessage::getReadFlag, 0));
    }

    @Override
    public void markRead(Long id) {
        update(new LambdaUpdateWrapper<SysMessage>()
                .eq(SysMessage::getId, id)
                .eq(SysMessage::getReceiverId, UserContext.getUserId())
                .set(SysMessage::getReadFlag, 1));
    }

    @Override
    public void markAllRead() {
        update(new LambdaUpdateWrapper<SysMessage>()
                .eq(SysMessage::getReceiverId, UserContext.getUserId())
                .eq(SysMessage::getReadFlag, 0)
                .set(SysMessage::getReadFlag, 1));
    }

    private static MessageDTO toDto(SysMessage message) {
        return new MessageDTO(message.getId(), message.getType(), message.getTitle(),
                message.getContent(), message.getBizId(), message.getReadFlag(), message.getCreateTime());
    }
}
