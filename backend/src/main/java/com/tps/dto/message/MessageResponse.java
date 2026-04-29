package com.tps.dto.message;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MessageResponse {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private String content;
    private String type;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
