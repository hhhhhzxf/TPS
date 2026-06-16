package com.tps.controller;

/**
 * 文件说明：控制器层，负责接收相关 HTTP 请求并委托业务层处理。
 */

import com.tps.dto.ApiResponse;
import com.tps.dto.user.UpdateProfileRequest;
import com.tps.dto.user.UserProfileResponse;
import com.tps.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final com.tps.service.ProductService productService;

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> getMe(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(userService.getProfile(userId));
    }

    @PutMapping("/me")
    public ApiResponse<UserProfileResponse> updateMe(@AuthenticationPrincipal Long userId,
                                                      @Valid @RequestBody UpdateProfileRequest req) {
        return ApiResponse.success(userService.updateProfile(userId, req));
    }

    @PutMapping("/me/avatar")
    public ApiResponse<UserProfileResponse> updateAvatar(@AuthenticationPrincipal Long userId,
                                                         @RequestParam String avatarUrl) {
        return ApiResponse.success(userService.updateAvatar(userId, avatarUrl));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserProfileResponse> getUser(@PathVariable Long id) {
        return ApiResponse.success(userService.getProfile(id));
    }

    @GetMapping("/{id}/products")
    public ApiResponse<?> getUserProducts(@PathVariable Long id) {
        return ApiResponse.success(productService.myProducts(id));
    }

    @PostMapping("/me/deactivate")
    public ApiResponse<?> deactivateMe(@AuthenticationPrincipal Long userId) {
        userService.deactivate(userId);
        return ApiResponse.success();
    }
}
