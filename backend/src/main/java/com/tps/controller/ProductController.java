package com.tps.controller;

/**
 * 文件说明：控制器层，负责接收相关 HTTP 请求并委托业务层处理。
 */

import com.tps.dto.ApiResponse;
import com.tps.dto.PageResponse;
import com.tps.dto.product.ProductRequest;
import com.tps.dto.product.ProductResponse;
import com.tps.dto.product.ReportProductRequest;
import com.tps.entity.Product;
import com.tps.service.BrowsingHistoryService;
import com.tps.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final BrowsingHistoryService browsingHistoryService;

    @GetMapping
    public ApiResponse<PageResponse<ProductResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String condition,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        if ((keyword != null && !keyword.isBlank())
                || (category != null && !category.isBlank())
                || minPrice != null
                || maxPrice != null
                || (condition != null && !condition.isBlank())) {
            return ApiResponse.success(PageResponse.from(productService.search(keyword, category, minPrice, maxPrice, condition, page, size, userId)));
        }
        return ApiResponse.success(PageResponse.from(productService.list(page, size, userId)));
    }

    @GetMapping("/search")
    public ApiResponse<PageResponse<ProductResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String condition,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        Long userId = extractUserId(authentication);
        return ApiResponse.success(PageResponse.from(productService.search(keyword, category, minPrice, maxPrice, condition, page, size, userId)));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> detail(@PathVariable Long id, Authentication authentication) {
        Long userId = extractUserId(authentication);
        if (userId != null) {
            browsingHistoryService.record(userId, id);
        }
        return ApiResponse.success(productService.getDetail(id, userId));
    }

    @PostMapping
    public ApiResponse<ProductResponse> create(@Valid @RequestBody ProductRequest req,
                                               @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(productService.create(userId, req));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody ProductRequest req,
                                               @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(productService.update(userId, id, req));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<ProductResponse> updateStatus(@PathVariable Long id,
                                                     @RequestParam String status,
                                                     @AuthenticationPrincipal Long userId) {
        productService.updateStatus(userId, id, Product.ProductStatus.valueOf(status));
        return ApiResponse.success(productService.getDetail(id, userId));
    }

    @PostMapping("/{id}/bump")
    public ApiResponse<ProductResponse> bump(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        return ApiResponse.success(productService.bump(userId, id));
    }

    @GetMapping("/my")
    public ApiResponse<List<ProductResponse>> myProducts(@AuthenticationPrincipal Long userId) {
        return ApiResponse.success(productService.myProducts(userId));
    }

    @PostMapping("/{id}/report")
    public ApiResponse<?> report(@PathVariable Long id,
                                 @RequestParam(required = false) String reason,
                                 @RequestBody(required = false) ReportProductRequest req,
                                 @AuthenticationPrincipal Long userId) {
        String reportReason = req != null && req.getReason() != null ? req.getReason() : reason;
        List<String> evidenceImageUrls = req != null ? req.getEvidenceImageUrls() : null;
        productService.report(userId, id, reportReason, evidenceImageUrls);
        return ApiResponse.success();
    }

    // 允许未登录访问，已登录则返回收藏状态
    private Long extractUserId(Authentication authentication) {
        return authentication != null && authentication.getPrincipal() instanceof Long userId ? userId : null;
    }
}
