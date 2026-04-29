package com.tps.dto.admin;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReportResponse {
    private Long id;
    private Long reporterId;
    private Long productId;
    private String productTitle;
    private String reason;
    private String status;
    private LocalDateTime createdAt;
}
