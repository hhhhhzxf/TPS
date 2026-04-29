package com.tps.dto.order;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReviewResponse {
    private Long id;
    private Long orderId;
    private Long reviewerId;
    private Long revieweeId;
    private Integer score;
    private String content;
    private LocalDateTime createdAt;
}
