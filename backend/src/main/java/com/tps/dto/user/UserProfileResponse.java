package com.tps.dto.user;

/**
 * 文件说明：数据传输对象，负责定义接口入参与出参结构。
 */

import lombok.Data;

@Data
public class UserProfileResponse {
    private Long id;
    private String phone;
    private String studentId;
    private String nickname;
    private String avatarUrl;
    private String bio;
    private String location;
    private String shippingAddress;
    private Integer creditScore;
    private String role;
    private String status;
    private boolean muted;
    private boolean publishBanned;
    private int productCount;
    private long reviewCount;
}
