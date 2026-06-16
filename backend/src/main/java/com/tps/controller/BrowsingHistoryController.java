package com.tps.controller;

/**
 * 文件说明：控制器层，负责接收相关 HTTP 请求并委托业务层处理。
 */

import com.tps.dto.ApiResponse;
import com.tps.dto.PageResponse;
import com.tps.service.BrowsingHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/history/products")
@RequiredArgsConstructor
public class BrowsingHistoryController {

    private final BrowsingHistoryService historyService;

    @PostMapping("/{productId}")
    public ApiResponse<?> record(@PathVariable Long productId, @AuthenticationPrincipal Long userId) {
        historyService.record(userId, productId);
        return ApiResponse.success();
    }

    @GetMapping
    public ApiResponse<?> list(@AuthenticationPrincipal Long userId,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(PageResponse.from(historyService.list(userId, page, size)));
    }

    @DeleteMapping
    public ApiResponse<?> clear(@AuthenticationPrincipal Long userId) {
        historyService.clear(userId);
        return ApiResponse.success();
    }

    @DeleteMapping("/{productId}")
    public ApiResponse<?> delete(@PathVariable Long productId, @AuthenticationPrincipal Long userId) {
        historyService.delete(userId, productId);
        return ApiResponse.success();
    }
}
