package com.tps.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank
    @Size(min = 6, max = 20, message = "密码长度6-20位")
    private String password;

    @NotBlank
    private String code; // 验证码，固定1234

    @NotBlank
    @Pattern(regexp = "^\\d+$", message = "学号必须为数字")
    @Size(min = 4, max = 32, message = "学号长度不正确")
    private String studentId;

    private String nickname;
}
