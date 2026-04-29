package com.tps.controller;

import com.tps.dto.ApiResponse;
import com.tps.dto.PageResponse;
import com.tps.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ApiResponse<?> createOrder(
            @AuthenticationPrincipal Long userId,
            @RequestParam Long productId,
            @RequestParam java.math.BigDecimal finalPrice) {
        return ApiResponse.success(orderService.createOrder(userId, productId, finalPrice));
    }

    @GetMapping("/my")
    public ApiResponse<?> myOrders(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "buyer") String role,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(PageResponse.from(orderService.myOrders(userId, role, status, page, size)));
    }

    @GetMapping("/{id}")
    public ApiResponse<?> getOrder(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(orderService.getOrder(id, userId));
    }

    @PutMapping("/{id}/pay")
    public ApiResponse<?> pay(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        orderService.pay(id, userId);
        return ApiResponse.success();
    }

    @PutMapping("/{id}/ship")
    public ApiResponse<?> ship(@PathVariable Long id,
                               @AuthenticationPrincipal Long userId,
                               @RequestParam(required = false) String trackingNumber) {
        orderService.ship(id, userId, trackingNumber);
        return ApiResponse.success();
    }

    @PutMapping("/{id}/confirm")
    public ApiResponse<?> confirm(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        orderService.confirm(id, userId);
        return ApiResponse.success();
    }

    @PutMapping("/{id}/cancel")
    public ApiResponse<?> cancel(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        orderService.cancel(id, userId);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/review")
    public ApiResponse<?> review(@PathVariable Long id,
                                 @AuthenticationPrincipal Long userId,
                                 @RequestParam Integer score,
                                 @RequestParam(required = false) String content) {
        return ApiResponse.success(orderService.review(id, userId, score, content));
    }

    @PostMapping("/{id}/refund")
    public ApiResponse<?> requestRefund(@PathVariable Long id,
                                        @AuthenticationPrincipal Long userId,
                                        @RequestParam(required = false) String reason) {
        orderService.requestRefund(id, userId, reason);
        return ApiResponse.success();
    }

    @PutMapping("/{id}/refund/approve")
    public ApiResponse<?> approveRefund(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        orderService.approveRefund(id, userId);
        return ApiResponse.success();
    }
}
