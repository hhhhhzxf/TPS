package com.tps.dto.message;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ConversationResponse {
    private Long id;
    private Long conversationId;
    private Long productId;
    private String productTitle;
    private BigDecimal productPrice;
    private String productImageUrl;
    private String productCover;
    private Long targetUserId;
    private String targetNickname;
    private String targetAvatarUrl;
    private String targetAvatar;
    private String lastMessage;
    private Integer unreadCount;
    private Integer unreadBuyer;
    private Integer unreadSeller;
    private LocalDateTime updatedAt;
}
