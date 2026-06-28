package com.ququ.im.service;

import com.ququ.im.dto.response.ImMessageEvent;
import com.ququ.im.dto.response.MessageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ImRealtimePublisher {
    private final SimpMessagingTemplate messagingTemplate;

    public void publishPrivateMessage(Long senderId, Long receiverId, MessageResponse message) {
        ImMessageEvent event = new ImMessageEvent("MESSAGE", message);
        messagingTemplate.convertAndSendToUser(String.valueOf(senderId), "/queue/messages", event);
        messagingTemplate.convertAndSendToUser(String.valueOf(receiverId), "/queue/messages", event);
    }

    public void publishGroupMessage(Long teamId, MessageResponse message) {
        messagingTemplate.convertAndSend("/topic/team/" + teamId, new ImMessageEvent("MESSAGE", message));
    }

    public void publishRecall(MessageResponse message) {
        ImMessageEvent event = new ImMessageEvent("RECALL", message);
        if (Integer.valueOf(1).equals(message.getConvType())) {
            messagingTemplate.convertAndSendToUser(String.valueOf(message.getSenderId()), "/queue/messages", event);
            messagingTemplate.convertAndSendToUser(String.valueOf(message.getReceiverId()), "/queue/messages", event);
            return;
        }
        messagingTemplate.convertAndSend("/topic/team/" + message.getTeamId(), event);
    }
}
