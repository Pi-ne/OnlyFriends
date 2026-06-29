package com.onlyfriends.im.controller;

import com.onlyfriends.common.exception.BizException;
import com.onlyfriends.common.response.ResultCode;
import com.onlyfriends.im.dto.request.GroupMessageRequest;
import com.onlyfriends.im.dto.request.PrivateMessageRequest;
import com.onlyfriends.im.dto.request.RecallMessageRequest;
import com.onlyfriends.im.security.StompPrincipal;
import com.onlyfriends.im.service.ImService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ImStompController {
    private final ImService imService;

    @MessageMapping("/chat.private")
    public void sendPrivate(Principal principal, @Valid @Payload PrivateMessageRequest request) {
        imService.sendPrivate(requireUserId(principal), request);
    }

    @MessageMapping("/chat.group")
    public void sendGroup(Principal principal, @Valid @Payload GroupMessageRequest request) {
        imService.sendGroup(requireUserId(principal), request);
    }

    @MessageMapping("/chat.recall")
    public void recall(Principal principal, @Valid @Payload RecallMessageRequest request) {
        imService.recall(requireUserId(principal), request.getMsgId());
    }

    private Long requireUserId(Principal principal) {
        if (principal instanceof StompPrincipal stompPrincipal) {
            return stompPrincipal.getUserId();
        }
        if (principal != null) {
            return Long.valueOf(principal.getName());
        }
        throw new BizException(ResultCode.UNAUTHORIZED);
    }
}
