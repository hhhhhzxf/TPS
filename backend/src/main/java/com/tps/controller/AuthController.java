package com.tps.controller;

import com.tps.dto.ApiResponse;
import com.tps.dto.auth.LoginRequest;
import com.tps.dto.auth.LoginResponse;
import com.tps.dto.auth.RefreshTokenRequest;
import com.tps.dto.auth.RegisterRequest;
import com.tps.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/code")
    public ApiResponse<?> sendCode(@RequestParam String phone) {
        authService.sendCode(phone);
        return ApiResponse.success("验证码已发送（固定：1234）");
    }

    @PostMapping("/register")
    public ApiResponse<LoginResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ApiResponse.success(authService.register(req));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return ApiResponse.success(authService.login(req));
    }

    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest req) {
        return ApiResponse.success(authService.refresh(req.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ApiResponse<?> logout() {
        return ApiResponse.success();
    }
}
