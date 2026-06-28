package com.ququ.im.config;

import com.ququ.im.security.StompPrincipal;
import com.ququ.im.service.ImStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

@Component
@RequiredArgsConstructor
public class ImWebSocketSessionListener {
    private final ImStateService stateService;

    @EventListener
    public void onConnect(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();
        if (principal instanceof StompPrincipal stompPrincipal) {
            stateService.markOnline(stompPrincipal.getUserId(), accessor.getSessionId());
        }
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        Principal principal = event.getUser();
        if (principal instanceof StompPrincipal stompPrincipal) {
            stateService.markOffline(stompPrincipal.getUserId(), event.getSessionId());
        }
    }
}
