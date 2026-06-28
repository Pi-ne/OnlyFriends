package com.ququ.im;

import com.ququ.common.dto.UserBasicDTO;
import com.ququ.im.dto.request.GroupMessageRequest;
import com.ququ.im.dto.request.PrivateMessageRequest;
import com.ququ.im.dto.response.MessageResponse;
import com.ququ.im.entity.Message;
import com.ququ.im.mapper.MessageMapper;
import com.ququ.im.service.ImRealtimePublisher;
import com.ququ.im.service.ImService;
import com.ququ.im.service.ImStateService;
import com.ququ.im.service.SocialClient;
import com.ququ.im.service.UserClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest
class ImServiceTest {
    @Autowired
    private ImService imService;

    @Autowired
    private MessageMapper messageMapper;

    @Autowired
    private ImStateService stateService;

    @MockBean
    private UserClient userClient;

    @MockBean
    private SocialClient socialClient;

    @MockBean
    private ImRealtimePublisher realtimePublisher;

    @Test
    void privateMessagePersistsAndPublishesRealtimeEvent() {
        mockUsers();
        when(userClient.isUserValid(20001L)).thenReturn(true);
        when(socialClient.areFriends(10001L, 20001L)).thenReturn(true);

        PrivateMessageRequest request = new PrivateMessageRequest();
        request.setReceiverId(20001L);
        request.setContent("你好，周末一起打球吗？");

        MessageResponse response = imService.sendPrivate(10001L, request);

        assertThat(response.getMsgId()).isNotNull();
        assertThat(response.getConvType()).isEqualTo(1);
        assertThat(response.getSenderId()).isEqualTo(10001L);
        assertThat(response.getReceiverId()).isEqualTo(20001L);
        assertThat(response.getContent()).isEqualTo("你好，周末一起打球吗？");

        Message saved = messageMapper.selectById(response.getMsgId());
        assertThat(saved.getContent()).isEqualTo("你好，周末一起打球吗？");
        verify(realtimePublisher).publishPrivateMessage(10001L, 20001L, response);
    }

    @Test
    void groupMessagePersistsAndPublishesToTeamTopic() {
        mockUsers();
        when(socialClient.isTeamMember(30001L, 10001L)).thenReturn(true);
        when(socialClient.getTeamMemberIds(30001L)).thenReturn(List.of(10001L, 20001L, 30001L));

        GroupMessageRequest request = new GroupMessageRequest();
        request.setTeamId(30001L);
        request.setContent("今晚 8 点线上碰一下活动流程。");
        request.setMsgType(10);
        request.setMentionAll(true);
        request.setMentionUserIds(List.of(20001L, 30001L));
        request.setRelatedType("announcement");
        request.setRelatedId(90001L);

        MessageResponse response = imService.sendGroup(10001L, request);

        assertThat(response.getMsgId()).isNotNull();
        assertThat(response.getConvType()).isEqualTo(2);
        assertThat(response.getTeamId()).isEqualTo(30001L);
        assertThat(response.getMsgType()).isEqualTo(10);
        assertThat(response.getMentionAll()).isTrue();
        assertThat(response.getMentionUserIds()).containsExactly(20001L, 30001L);
        assertThat(response.getRelatedType()).isEqualTo("announcement");
        assertThat(response.getRelatedId()).isEqualTo(90001L);
        verify(realtimePublisher).publishGroupMessage(30001L, response);
    }

    @Test
    void recallUpdatesMessageAndPublishesRecallEvent() {
        mockUsers();
        when(userClient.isUserValid(20001L)).thenReturn(true);
        when(socialClient.areFriends(10001L, 20001L)).thenReturn(true);

        PrivateMessageRequest request = new PrivateMessageRequest();
        request.setReceiverId(20001L);
        request.setContent("这条稍后撤回");
        MessageResponse sent = imService.sendPrivate(10001L, request);

        imService.recall(10001L, sent.getMsgId());

        Message recalled = messageMapper.selectById(sent.getMsgId());
        assertThat(recalled.getStatus()).isEqualTo(2);
        assertThat(recalled.getContent()).isEmpty();
        verify(realtimePublisher).publishRecall(org.mockito.ArgumentMatchers.argThat(event ->
                event.getMsgId().equals(sent.getMsgId()) && event.getStatus().equals(2)));
    }

    @Test
    void unreadCacheAndOnlineStateWorkWithLocalFallback() {
        mockUsers();
        when(userClient.isUserValid(21002L)).thenReturn(true);
        when(socialClient.areFriends(11001L, 21002L)).thenReturn(true);

        assertThat(stateService.isOnline(21002L)).isFalse();
        stateService.markOnline(21002L, "session-a");
        assertThat(stateService.isOnline(21002L)).isTrue();

        PrivateMessageRequest request = new PrivateMessageRequest();
        request.setReceiverId(21002L);
        request.setContent("未读数测试");
        MessageResponse sent = imService.sendPrivate(11001L, request);

        assertThat(imService.conversations(21002L))
                .filteredOn(conversation -> conversation.getConvId().equals(sent.getConvId()))
                .singleElement()
                .extracting("unreadCount")
                .isEqualTo(1L);

        imService.markRead(21002L, sent.getConvId(), sent.getMsgId());
        assertThat(imService.conversations(21002L))
                .filteredOn(conversation -> conversation.getConvId().equals(sent.getConvId()))
                .singleElement()
                .extracting("unreadCount")
                .isEqualTo(0L);

        stateService.markOffline(21002L, "session-a");
        assertThat(stateService.isOnline(21002L)).isFalse();
    }

    private void mockUsers() {
        when(userClient.getUsersByIds(anyList())).thenAnswer(invocation -> {
            List<Long> ids = invocation.getArgument(0);
            return ids.stream()
                    .distinct()
                    .map(id -> new UserBasicDTO(id, "user-" + id, "/avatar/" + id + ".png", 0))
                    .collect(Collectors.toMap(UserBasicDTO::getUserId, Function.identity(), (left, right) -> left));
        });
        when(userClient.isUserValid(anyLong())).thenReturn(true);
    }
}
