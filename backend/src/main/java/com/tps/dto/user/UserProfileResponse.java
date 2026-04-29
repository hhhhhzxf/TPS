package com.tps.dto.user;

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
    private int productCount;
}
