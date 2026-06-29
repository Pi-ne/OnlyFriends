package com.onlyfriends.im.service;

import com.onlyfriends.common.dto.PageResult;
import com.onlyfriends.im.dto.request.GroupMessageRequest;
import com.onlyfriends.im.dto.request.PrivateMessageRequest;
import com.onlyfriends.im.dto.response.ConversationResponse;
import com.onlyfriends.im.dto.response.MessageResponse;

import java.util.List;

public interface ImService {
    MessageResponse sendPrivate(Long senderId, PrivateMessageRequest request);

    MessageResponse sendGroup(Long senderId, GroupMessageRequest request);

    List<ConversationResponse> conversations(Long userId);

    PageResult<MessageResponse> messages(Long userId, Long convId, Integer page, Integer size);

    PageResult<MessageResponse> groupMessages(Long userId, Long teamId, Integer page, Integer size);

    void recall(Long userId, Long msgId);

    void markRead(Long userId, Long convId, Long lastReadMsgId);
}
