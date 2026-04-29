package com.tps.service;

import com.tps.dto.admin.ReportResponse;
import com.tps.dto.order.OrderResponse;
import com.tps.dto.user.UserProfileResponse;
import com.tps.entity.*;
import com.tps.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ReportRepository reportRepository;
    private final NotificationRepository notificationRepository;

    public Page<UserProfileResponse> getUsers(int page, int size) {
        return userRepository.findAll(PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(this::toUserProfile);
    }

    @Transactional
    public void banUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        user.setStatus(User.UserStatus.BANNED);
        userRepository.save(user);
    }

    @Transactional
    public void unbanUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        user.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(user);
    }

    private final OrderService orderService;

    public Page<ReportResponse> getReportedProducts(int page, int size) {
        return reportRepository.findByStatus(Report.ReportStatus.PENDING,
                PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(this::toReportResponse);
    }

    @Transactional
    public void takedownProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        product.setStatus(Product.ProductStatus.OFF);
        productRepository.save(product);
    }

    public Page<OrderResponse> getOrders(int page, int size) {
        return orderRepository.findAll(PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(orderService::toResponse);
    }

    public Page<OrderResponse> getRefundingOrders(int page, int size) {
        return orderRepository.findByStatus(Order.OrderStatus.REFUNDING,
                PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(orderService::toResponse);
    }

    @Transactional
    public void handleReport(Long reportId, boolean takedown) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("举报不存在"));
        if (takedown) {
            takedownProduct(report.getProductId());
        }
        report.setStatus(Report.ReportStatus.HANDLED);
        reportRepository.save(report);
    }

    @Transactional
    public void approveRefund(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        if (order.getStatus() != Order.OrderStatus.REFUNDING) {
            throw new IllegalArgumentException("订单不在退款中");
        }
        order.setStatus(Order.OrderStatus.REFUNDED);
        productRepository.findById(order.getProductId()).ifPresent(product -> {
            product.setStatus(Product.ProductStatus.ON_SALE);
            productRepository.save(product);
        });
        orderRepository.save(order);
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalProducts", productRepository.count());
        stats.put("totalOrders", orderRepository.count());
        stats.put("totalAmount", orderRepository.sumFinalPrice());
        return stats;
    }

    private UserProfileResponse toUserProfile(User user) {
        UserProfileResponse response = new UserProfileResponse();
        response.setId(user.getId());
        response.setPhone(user.getPhone());
        response.setStudentId(user.getStudentId());
        response.setNickname(user.getNickname());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setBio(user.getBio());
        response.setLocation(user.getLocation());
        response.setShippingAddress(user.getShippingAddress());
        response.setCreditScore(user.getCreditScore());
        response.setRole(user.getRole().name());
        response.setStatus(user.getStatus().name());
        response.setProductCount(productRepository.findByUserId(user.getId()).size());
        return response;
    }

    private ReportResponse toReportResponse(Report report) {
        ReportResponse response = new ReportResponse();
        response.setId(report.getId());
        response.setReporterId(report.getReporterId());
        response.setProductId(report.getProductId());
        productRepository.findById(report.getProductId())
                .ifPresent(product -> response.setProductTitle(product.getTitle()));
        response.setReason(report.getReason());
        response.setStatus(report.getStatus().name());
        response.setCreatedAt(report.getCreatedAt());
        return response;
    }

    @Transactional
    public void sendAnnouncement(String content) {
        // 给所有用户创建系统通知
        userRepository.findAll().forEach(user -> {
            Notification n = new Notification();
            n.setUserId(user.getId());
            n.setType("SYSTEM");
            n.setContent(content);
            notificationRepository.save(n);
        });
    }
}
