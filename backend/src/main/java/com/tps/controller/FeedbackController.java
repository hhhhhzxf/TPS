package com.tps.controller;

/**
 * 文件说明：控制器层，负责接收相关 HTTP 请求并委托业务层处理。
 */

import com.tps.dto.ApiResponse;
import com.tps.dto.PageResponse;
import com.tps.dto.feedback.FeedbackRequest;
import com.tps.service.FeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    public ApiResponse<?> create(@Valid @RequestBody FeedbackRequest request,
                                 @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(feedbackService.create(userId, request));
    }

    @GetMapping("/my")
    public ApiResponse<?> my(@AuthenticationPrincipal Long userId,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(PageResponse.from(feedbackService.my(userId, page, size)));
    }

    @GetMapping("/{id}")
    public ApiResponse<?> get(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(feedbackService.get(userId, id));
    }
}
