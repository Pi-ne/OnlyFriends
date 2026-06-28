package com.ququ.im.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImMessageEvent {
    private String eventType;
    private MessageResponse message;
}
