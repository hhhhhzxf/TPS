package com.tps.websocket;

import lombok.Data;

@Data
public class ChatMessage {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private String content;
    private String type; // TEXT, IMAGE
    private String createdAt;
}
