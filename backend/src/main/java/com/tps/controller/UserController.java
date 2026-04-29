package com.tps.controller;

import com.tps.dto.ApiResponse;
import com.tps.dto.user.UpdateProfileRequest;
import com.tps.dto.user.UserProfileResponse;
import com.tps.security.JwtUtil;
import com.tps.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final com.tps.service.ProductService productService;

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> getMe(HttpServletRequest request) {
        Long userId = getUserId(request);
        return ApiResponse.success(userService.getProfile(userId));
    }

    @PutMapping("/me")
    public ApiResponse<UserProfileResponse> updateMe(HttpServletRequest request,
                                                      @Valid @RequestBody UpdateProfileRequest req) {
        Long userId = getUserId(request);
        return ApiResponse.success(userService.updateProfile(userId, req));
    }

    @PutMapping("/me/avatar")
    public ApiResponse<UserProfileResponse> updateAvatar(HttpServletRequest request,
                                                         @RequestParam String avatarUrl) {
        Long userId = getUserId(request);
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
    public ApiResponse<?> deactivateMe(HttpServletRequest request) {
        Long userId = getUserId(request);
        userService.deactivate(userId);
        return ApiResponse.success();
    }

    private Long getUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        return jwtUtil.getUserId(token);
    }
}
