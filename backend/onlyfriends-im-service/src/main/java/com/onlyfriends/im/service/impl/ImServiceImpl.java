package com.onlyfriends.im.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlyfriends.common.dto.PageResult;
import com.onlyfriends.common.dto.UserBasicDTO;
import com.onlyfriends.common.exception.BizException;
import com.onlyfriends.common.response.ResultCode;
import com.onlyfriends.im.dto.request.GroupMessageRequest;
import com.onlyfriends.im.dto.request.PrivateMessageRequest;
import com.onlyfriends.im.dto.response.ConversationResponse;
import com.onlyfriends.im.dto.response.MessageResponse;
import com.onlyfriends.im.entity.Conversation;
import com.onlyfriends.im.entity.ConversationRead;
import com.onlyfriends.im.entity.Message;
import com.onlyfriends.im.mapper.ConversationMapper;
import com.onlyfriends.im.mapper.ConversationReadMapper;
import com.onlyfriends.im.mapper.MessageMapper;
import com.onlyfriends.im.service.ImRealtimePublisher;
import com.onlyfriends.im.service.ImService;
import com.onlyfriends.im.service.ImStateService;
import com.onlyfriends.im.service.SocialClient;
import com.onlyfriends.im.service.UserClient;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ImServiceImpl implements ImService {
    private static final int CONV_PRIVATE = 1;
    private static final int CONV_GROUP = 2;
    private static final int MSG_TEXT = 1;
    private static final int MSG_NORMAL = 1;
    private static final int MSG_RECALLED = 2;
    private static final long RECALL_LIMIT_SECONDS = 120;

    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final ConversationReadMapper readMapper;
    private final UserClient userClient;
    private final SocialClient socialClient;
    private final ImRealtimePublisher realtimePublisher;
    private final ImStateService stateService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public MessageResponse sendPrivate(Long senderId, PrivateMessageRequest request) {
        if (senderId.equals(request.getReceiverId())) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "不能给自己发送私聊消息");
        }
        if (!userClient.isUserValid(request.getReceiverId())) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "接收人不存在或状态异常");
        }
        if (!socialClient.areFriends(senderId, request.getReceiverId())) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "非好友不能发送私聊消息");
        }
        Conversation conversation = getOrCreatePrivateConversation(senderId, request.getReceiverId());
        Message message = insertMessage(conversation.getId(), CONV_PRIVATE, senderId, request.getReceiverId(), null,
                request.getMsgType(), request.getContent(), false, null, null, null);
        updateConversation(conversation, message);
        stateService.incrementUnread(conversation.getId(), List.of(request.getReceiverId()), senderId);
        MessageResponse response = toMessageResponse(message, senderId, userClient.getUsersByIds(List.of(senderId)));
        realtimePublisher.publishPrivateMessage(senderId, request.getReceiverId(), response);
        return response;
    }

    @Override
    @Transactional
    public MessageResponse sendGroup(Long senderId, GroupMessageRequest request) {
        if (!socialClient.isTeamMember(request.getTeamId(), senderId)) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "非小队成员不能发送群聊消息");
        }
        Conversation conversation = getOrCreateGroupConversation(request.getTeamId());
        Message message = insertMessage(conversation.getId(), CONV_GROUP, senderId, null, request.getTeamId(),
                request.getMsgType(), request.getContent(), Boolean.TRUE.equals(request.getMentionAll()),
                request.getMentionUserIds(), request.getRelatedType(), request.getRelatedId());
        updateConversation(conversation, message);
        stateService.incrementUnread(conversation.getId(), socialClient.getTeamMemberIds(request.getTeamId()), senderId);
        MessageResponse response = toMessageResponse(message, senderId, userClient.getUsersByIds(List.of(senderId)));
        realtimePublisher.publishGroupMessage(request.getTeamId(), response);
        return response;
    }

    @Override
    public List<ConversationResponse> conversations(Long userId) {
        List<Conversation> privateConversations = conversationMapper.selectList(new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getConvType, CONV_PRIVATE)
                .and(w -> w.eq(Conversation::getUserIdA, userId).or().eq(Conversation::getUserIdB, userId)));
        List<Conversation> groupConversations = conversationMapper.selectList(new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getConvType, CONV_GROUP));
        List<Conversation> accessibleGroups = groupConversations.stream()
                .filter(conversation -> socialClient.isTeamMember(conversation.getTeamId(), userId))
                .toList();
        List<Conversation> all = new java.util.ArrayList<>();
        all.addAll(privateConversations);
        all.addAll(accessibleGroups);
        all.sort(Comparator.comparing(Conversation::getLastMsgAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed());

        List<Long> userIds = all.stream()
                .filter(conversation -> Integer.valueOf(CONV_PRIVATE).equals(conversation.getConvType()))
                .map(conversation -> conversation.getUserIdA().equals(userId) ? conversation.getUserIdB() : conversation.getUserIdA())
                .distinct()
                .toList();
        Map<Long, UserBasicDTO> users = userClient.getUsersByIds(userIds);
        return all.stream().map(conversation -> toConversationResponse(conversation, userId, users)).toList();
    }

    @Override
    public PageResult<MessageResponse> messages(Long userId, Long convId, Integer page, Integer size) {
        Conversation conversation = getConversationOrThrow(convId);
        ensureConversationMember(conversation, userId);
        int current = page == null || page < 1 ? 1 : page;
        int pageSize = size == null ? 30 : Math.min(Math.max(size, 1), 100);
        List<Message> all = messageMapper.selectList(new LambdaQueryWrapper<Message>()
                .eq(Message::getConvId, convId)
                .orderByDesc(Message::getCreatedAt)
                .orderByDesc(Message::getId));
        List<Message> rows = all.stream()
                .skip((long) (current - 1) * pageSize)
                .limit(pageSize)
                .sorted(Comparator.comparing(Message::getCreatedAt).thenComparing(Message::getId))
                .toList();
        Map<Long, UserBasicDTO> users = userClient.getUsersByIds(rows.stream().map(Message::getSenderId).distinct().toList());
        List<MessageResponse> responses = rows.stream().map(message -> toMessageResponse(message, userId, users)).toList();
        return new PageResult<>(responses, (long) all.size(), (long) current, (long) pageSize);
    }

    @Override
    public PageResult<MessageResponse> groupMessages(Long userId, Long teamId, Integer page, Integer size) {
        if (!socialClient.isTeamMember(teamId, userId)) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "无权访问该群聊会话");
        }
        Conversation conversation = getOrCreateGroupConversation(teamId);
        return messages(userId, conversation.getId(), page, size);
    }

    @Override
    @Transactional
    public void recall(Long userId, Long msgId) {
        Message message = messageMapper.selectById(msgId);
        if (message == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "消息不存在");
        }
        if (!message.getSenderId().equals(userId)) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "只能撤回自己发送的消息");
        }
        if (!Integer.valueOf(MSG_NORMAL).equals(message.getStatus())) {
            return;
        }
        if (Duration.between(message.getCreatedAt(), LocalDateTime.now()).getSeconds() > RECALL_LIMIT_SECONDS) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "消息发送超过2分钟，不能撤回");
        }
        message.setStatus(MSG_RECALLED);
        message.setContent("");
        message.setRecalledAt(LocalDateTime.now());
        messageMapper.updateById(message);
        Conversation conversation = conversationMapper.selectById(message.getConvId());
        if (conversation != null && message.getId().equals(conversation.getLastMsgId())) {
            conversation.setLastMsgPreview("[消息已撤回]");
            conversationMapper.updateById(conversation);
        }
        realtimePublisher.publishRecall(toMessageResponse(message, userId, userClient.getUsersByIds(List.of(userId))));
    }

    @Override
    @Transactional
    public void markRead(Long userId, Long convId, Long lastReadMsgId) {
        Conversation conversation = getConversationOrThrow(convId);
        ensureConversationMember(conversation, userId);
        Message message = messageMapper.selectById(lastReadMsgId);
        if (message == null || !message.getConvId().equals(convId)) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "已读消息不属于该会话");
        }
        ConversationRead read = readMapper.selectOne(new LambdaQueryWrapper<ConversationRead>()
                .eq(ConversationRead::getConvId, convId)
                .eq(ConversationRead::getUserId, userId)
                .last("LIMIT 1"));
        if (read == null) {
            read = new ConversationRead();
            read.setConvId(convId);
            read.setUserId(userId);
            read.setLastReadMsgId(lastReadMsgId);
            read.setReadAt(LocalDateTime.now());
            readMapper.insert(read);
        } else if (read.getLastReadMsgId() == null || lastReadMsgId > read.getLastReadMsgId()) {
            read.setLastReadMsgId(lastReadMsgId);
            read.setReadAt(LocalDateTime.now());
            readMapper.updateById(read);
        }
        stateService.clearUnread(convId, userId);
    }

    private Conversation getOrCreatePrivateConversation(Long userIdA, Long userIdB) {
        Long min = Math.min(userIdA, userIdB);
        Long max = Math.max(userIdA, userIdB);
        Conversation existing = conversationMapper.selectOne(new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getConvType, CONV_PRIVATE)
                .eq(Conversation::getUserIdA, min)
                .eq(Conversation::getUserIdB, max)
                .last("LIMIT 1"));
        if (existing != null) {
            return existing;
        }
        Conversation conversation = new Conversation();
        conversation.setConvType(CONV_PRIVATE);
        conversation.setUserIdA(min);
        conversation.setUserIdB(max);
        try {
            conversationMapper.insert(conversation);
            return conversation;
        } catch (DuplicateKeyException ex) {
            return conversationMapper.selectOne(new LambdaQueryWrapper<Conversation>()
                    .eq(Conversation::getConvType, CONV_PRIVATE)
                    .eq(Conversation::getUserIdA, min)
                    .eq(Conversation::getUserIdB, max)
                    .last("LIMIT 1"));
        }
    }

    private Conversation getOrCreateGroupConversation(Long teamId) {
        Conversation existing = conversationMapper.selectOne(new LambdaQueryWrapper<Conversation>()
                .eq(Conversation::getConvType, CONV_GROUP)
                .eq(Conversation::getTeamId, teamId)
                .last("LIMIT 1"));
        if (existing != null) {
            return existing;
        }
        Conversation conversation = new Conversation();
        conversation.setConvType(CONV_GROUP);
        conversation.setTeamId(teamId);
        try {
            conversationMapper.insert(conversation);
            return conversation;
        } catch (DuplicateKeyException ex) {
            return conversationMapper.selectOne(new LambdaQueryWrapper<Conversation>()
                    .eq(Conversation::getConvType, CONV_GROUP)
                    .eq(Conversation::getTeamId, teamId)
                    .last("LIMIT 1"));
        }
    }

    private Message insertMessage(Long convId, Integer convType, Long senderId, Long receiverId, Long teamId,
                                  Integer msgType, String content, Boolean mentionAll, List<Long> mentionUserIds,
                                  String relatedType, Long relatedId) {
        Message message = new Message();
        message.setConvId(convId);
        message.setConvType(convType);
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setTeamId(teamId);
        message.setMsgType(msgType == null ? MSG_TEXT : msgType);
        message.setContent(content.trim());
        message.setMentionAll(Boolean.TRUE.equals(mentionAll));
        message.setMentionUserIds(toJson(mentionUserIds));
        message.setRelatedType(StringUtils.hasText(relatedType) ? relatedType.trim() : null);
        message.setRelatedId(relatedId);
        message.setStatus(MSG_NORMAL);
        messageMapper.insert(message);
        return message;
    }

    private void updateConversation(Conversation conversation, Message message) {
        conversation.setLastMsgId(message.getId());
        conversation.setLastMsgPreview(preview(message));
        conversation.setLastMsgAt(message.getCreatedAt());
        conversationMapper.updateById(conversation);
    }

    private Conversation getConversationOrThrow(Long convId) {
        Conversation conversation = conversationMapper.selectById(convId);
        if (conversation == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "会话不存在");
        }
        return conversation;
    }

    private void ensureConversationMember(Conversation conversation, Long userId) {
        if (Integer.valueOf(CONV_PRIVATE).equals(conversation.getConvType())) {
            if (!conversation.getUserIdA().equals(userId) && !conversation.getUserIdB().equals(userId)) {
                throw new BizException(ResultCode.FORBIDDEN.getCode(), "无权访问该私聊会话");
            }
            return;
        }
        if (!socialClient.isTeamMember(conversation.getTeamId(), userId)) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "无权访问该群聊会话");
        }
    }

    private ConversationResponse toConversationResponse(Conversation conversation, Long currentUserId, Map<Long, UserBasicDTO> users) {
        ConversationResponse response = new ConversationResponse();
        response.setConvId(conversation.getId());
        response.setConvType(conversation.getConvType());
        response.setTeamId(conversation.getTeamId());
        response.setLastMsgId(conversation.getLastMsgId());
        response.setLastMsgPreview(conversation.getLastMsgPreview());
        response.setLastMsgAt(conversation.getLastMsgAt());
        if (Integer.valueOf(CONV_PRIVATE).equals(conversation.getConvType())) {
            Long peerId = conversation.getUserIdA().equals(currentUserId) ? conversation.getUserIdB() : conversation.getUserIdA();
            UserBasicDTO peer = users.get(peerId);
            response.setPeerUserId(peerId);
            response.setPeerNickname(peer == null ? null : peer.getNickname());
            response.setPeerAvatarUrl(peer == null ? null : peer.getAvatarUrl());
            response.setTitle(peer == null ? "用户" + peerId : peer.getNickname());
        } else {
            response.setTitle("小队群聊 " + conversation.getTeamId());
        }
        response.setUnreadCount(stateService.getUnread(conversation.getId(), currentUserId,
                () -> unreadCount(conversation.getId(), currentUserId)));
        return response;
    }

    private MessageResponse toMessageResponse(Message message, Long currentUserId, Map<Long, UserBasicDTO> users) {
        UserBasicDTO sender = users.get(message.getSenderId());
        MessageResponse response = new MessageResponse();
        response.setMsgId(message.getId());
        response.setConvId(message.getConvId());
        response.setConvType(message.getConvType());
        response.setSenderId(message.getSenderId());
        response.setSenderNickname(sender == null ? null : sender.getNickname());
        response.setSenderAvatarUrl(sender == null ? null : sender.getAvatarUrl());
        response.setReceiverId(message.getReceiverId());
        response.setTeamId(message.getTeamId());
        response.setMsgType(message.getMsgType());
        response.setContent(Integer.valueOf(MSG_RECALLED).equals(message.getStatus()) ? "" : message.getContent());
        response.setMentionAll(Boolean.TRUE.equals(message.getMentionAll()));
        response.setMentionUserIds(fromJson(message.getMentionUserIds()));
        response.setRelatedType(message.getRelatedType());
        response.setRelatedId(message.getRelatedId());
        response.setStatus(message.getStatus());
        response.setMine(message.getSenderId().equals(currentUserId));
        response.setRecalledAt(message.getRecalledAt());
        response.setCreatedAt(message.getCreatedAt());
        return response;
    }

    private Long unreadCount(Long convId, Long userId) {
        ConversationRead read = readMapper.selectOne(new LambdaQueryWrapper<ConversationRead>()
                .eq(ConversationRead::getConvId, convId)
                .eq(ConversationRead::getUserId, userId)
                .last("LIMIT 1"));
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<Message>()
                .eq(Message::getConvId, convId)
                .ne(Message::getSenderId, userId);
        if (read != null && read.getLastReadMsgId() != null) {
            wrapper.gt(Message::getId, read.getLastReadMsgId());
        }
        return messageMapper.selectCount(wrapper);
    }

    private String preview(Message message) {
        if (Integer.valueOf(MSG_RECALLED).equals(message.getStatus())) {
            return "[消息已撤回]";
        }
        if (!StringUtils.hasText(message.getContent())) {
            return "";
        }
        return message.getContent().length() > 50 ? message.getContent().substring(0, 50) : message.getContent();
    }

    private String toJson(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(ids.stream().distinct().toList());
        } catch (JsonProcessingException ex) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "消息@用户列表格式错误");
        }
    }

    private List<Long> fromJson(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException ex) {
            return Collections.emptyList();
        }
    }
}
