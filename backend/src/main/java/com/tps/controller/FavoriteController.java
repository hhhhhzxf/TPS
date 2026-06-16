package com.tps.controller;

/**
 * 文件说明：控制器层，负责接收相关 HTTP 请求并委托业务层处理。
 */

import com.tps.dto.ApiResponse;
import com.tps.dto.product.ProductResponse;
import com.tps.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping("/{productId}")
    public ApiResponse<Boolean> add(@PathVariable Long productId, @AuthenticationPrincipal Long userId) {
        favoriteService.add(userId, productId);
        return ApiResponse.success(true);
    }

    @DeleteMapping("/{productId}")
    public ApiResponse<Boolean> remove(@PathVariable Long productId, @AuthenticationPrincipal Long userId) {
        favoriteService.remove(userId, productId);
        return ApiResponse.success(false);
    }

    @PostMapping("/{productId}/toggle")
    public ApiResponse<Boolean> toggle(@PathVariable Long productId, @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(favoriteService.toggle(userId, productId));
    }

    @GetMapping("/{productId}/status")
    public ApiResponse<Boolean> status(@PathVariable Long productId, @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(favoriteService.isFavorited(userId, productId));
    }

    @GetMapping
    public ApiResponse<List<ProductResponse>> myFavorites(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(favoriteService.myFavorites(userId));
    }
}
