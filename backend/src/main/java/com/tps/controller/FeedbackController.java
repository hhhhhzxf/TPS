package com.tps.controller;

import com.tps.dto.ApiResponse;
import com.tps.dto.PageResponse;
import com.tps.dto.feedback.FeedbackRequest;
import com.tps.security.JwtUtil;
import com.tps.service.FeedbackService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final JwtUtil jwtUtil;

    @PostMapping
    public ApiResponse<?> create(@Valid @RequestBody FeedbackRequest request,
                                 HttpServletRequest httpRequest) {
        return ApiResponse.success(feedbackService.create(getUserId(httpRequest), request));
    }

    @GetMapping("/my")
    public ApiResponse<?> my(HttpServletRequest request,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(PageResponse.from(feedbackService.my(getUserId(request), page, size)));
    }

    @GetMapping("/{id}")
    public ApiResponse<?> get(@PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.success(feedbackService.get(getUserId(request), id));
    }

    private Long getUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        return jwtUtil.getUserId(token);
    }
}
