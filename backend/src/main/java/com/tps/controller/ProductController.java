package com.tps.controller;

import com.tps.dto.ApiResponse;
import com.tps.dto.PageResponse;
import com.tps.dto.product.ProductRequest;
import com.tps.dto.product.ProductResponse;
import com.tps.entity.Product;
import com.tps.security.JwtUtil;
import com.tps.service.BrowsingHistoryService;
import com.tps.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final BrowsingHistoryService browsingHistoryService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public ApiResponse<PageResponse<ProductResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String condition,
            HttpServletRequest request) {
        Long userId = extractUserId(request);
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
            HttpServletRequest request) {
        Long userId = extractUserId(request);
        return ApiResponse.success(PageResponse.from(productService.search(keyword, category, minPrice, maxPrice, condition, page, size, userId)));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> detail(@PathVariable Long id, HttpServletRequest request) {
        Long userId = extractUserId(request);
        if (userId != null) {
            browsingHistoryService.record(userId, id);
        }
        return ApiResponse.success(productService.getDetail(id, userId));
    }

    @PostMapping
    public ApiResponse<ProductResponse> create(@Valid @RequestBody ProductRequest req,
                                               HttpServletRequest request) {
        Long userId = getUserId(request);
        return ApiResponse.success(productService.create(userId, req));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody ProductRequest req,
                                               HttpServletRequest request) {
        Long userId = getUserId(request);
        return ApiResponse.success(productService.update(userId, id, req));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<ProductResponse> updateStatus(@PathVariable Long id,
                                                     @RequestParam String status,
                                                     HttpServletRequest request) {
        Long userId = getUserId(request);
        productService.updateStatus(userId, id, Product.ProductStatus.valueOf(status));
        return ApiResponse.success(productService.getDetail(id, userId));
    }

    @PostMapping("/{id}/bump")
    public ApiResponse<ProductResponse> bump(@PathVariable Long id, HttpServletRequest request) {
        Long userId = getUserId(request);
        return ApiResponse.success(productService.bump(userId, id));
    }

    @GetMapping("/my")
    public ApiResponse<List<ProductResponse>> myProducts(HttpServletRequest request) {
        Long userId = getUserId(request);
        return ApiResponse.success(productService.myProducts(userId));
    }

    @PostMapping("/{id}/report")
    public ApiResponse<?> report(@PathVariable Long id,
                                 @RequestParam String reason,
                                 HttpServletRequest request) {
        Long userId = getUserId(request);
        productService.report(userId, id, reason);
        return ApiResponse.success();
    }

    private Long getUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization").substring(7);
        return jwtUtil.getUserId(token);
    }

    // 允许未登录访问，已登录则返回收藏状态
    private Long extractUserId(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            try { return jwtUtil.getUserId(auth.substring(7)); } catch (Exception ignored) {}
        }
        return null;
    }
}
