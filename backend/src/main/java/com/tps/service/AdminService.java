package com.tps.service;

/**
 * 文件说明：业务服务层，负责封装核心业务规则、事务与对象组装。
 */

import com.tps.dto.admin.ReportResponse;
import com.tps.dto.order.OrderResponse;
import com.tps.dto.product.ProductResponse;
import com.tps.dto.user.UserProfileResponse;
import com.tps.entity.*;
import com.tps.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ReportRepository reportRepository;
    private final NotificationRepository notificationRepository;
    private final ProductService productService;
    private final ProductImageRepository productImageRepository;
    private final FileService fileService;

    public Page<UserProfileResponse> getUsers(int page, int size) {
        return getUsers(null, null, page, size);
    }

    public Page<UserProfileResponse> getUsers(String status, String keyword, int page, int size) {
        return getUsers(status, keyword, "createdAt", "desc", page, size);
    }

    public Page<UserProfileResponse> getUsers(String status, String keyword, String sort, String direction, int page, int size) {
        Specification<User> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (hasFilter(status)) {
                predicates.add(cb.equal(root.get("status"), User.UserStatus.valueOf(status.trim().toUpperCase(Locale.ROOT))));
            }
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.<String>get("phone")), pattern),
                        cb.like(cb.lower(root.<String>get("studentId")), pattern),
                        cb.like(cb.lower(root.<String>get("nickname")), pattern)
                ));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        return userRepository.findAll(spec, pageable(page, size, normalizeUserSort(sort), direction))
                .map(this::toUserProfile);
    }

    @Transactional
    public void banUser(Long userId) {
        User user = getMutableUser(userId, "管理员账号不能被封禁");
        user.setStatus(User.UserStatus.BANNED);
        userRepository.save(user);
    }

    @Transactional
    public void unbanUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        if (user.getRole() == User.Role.ADMIN) {
            user.setStatus(User.UserStatus.ACTIVE);
            userRepository.save(user);
            return;
        }
        user.setStatus(User.UserStatus.ACTIVE);
        userRepository.save(user);
    }

    @Transactional
    public void muteUser(Long userId) {
        User user = getMutableUser(userId, "管理员账号不能被禁言");
        user.setMuted(true);
        userRepository.save(user);
    }

    @Transactional
    public void unmuteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        user.setMuted(false);
        userRepository.save(user);
    }

    @Transactional
    public void publishBanUser(Long userId) {
        User user = getMutableUser(userId, "管理员账号不能被禁止发布商品");
        user.setPublishBanned(true);
        userRepository.save(user);
    }

    @Transactional
    public void publishUnbanUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        user.setPublishBanned(false);
        userRepository.save(user);
    }

    private final OrderService orderService;

    public Page<ProductResponse> getProducts(String status, int page, int size) {
        return getProducts(status, null, null, null, page, size);
    }

    public Page<ProductResponse> getProducts(String status, String keyword, String category, Long sellerId, int page, int size) {
        Specification<Product> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (hasFilter(status)) {
                predicates.add(cb.equal(root.get("status"), Product.ProductStatus.valueOf(status.trim().toUpperCase(Locale.ROOT))));
            }
            if (category != null && !category.isBlank()) {
                predicates.add(cb.equal(root.get("category"), category.trim()));
            }
            if (sellerId != null) {
                predicates.add(cb.equal(root.get("userId"), sellerId));
            }
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.<String>get("title")), pattern),
                        cb.like(cb.lower(root.<String>get("description")), pattern)
                ));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        return productRepository.findAll(spec, pageable(page, size, "createdAt"))
                .map(product -> productService.toResponse(product, null));
    }

    public ProductResponse getProductDetail(Long productId) {
        return productService.getDetailWithoutIncrement(productId, null);
    }

    public Page<ReportResponse> getReportedProducts(int page, int size) {
        return getReports(Report.ReportStatus.PENDING.name(), null, null, page, size);
    }

    public Page<ReportResponse> getReports(String status, Long productId, Long reporterId, int page, int size) {
        Specification<Report> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (hasFilter(status)) {
                predicates.add(cb.equal(root.get("status"), Report.ReportStatus.valueOf(status.trim().toUpperCase(Locale.ROOT))));
            }
            if (productId != null) {
                predicates.add(cb.equal(root.get("productId"), productId));
            }
            if (reporterId != null) {
                predicates.add(cb.equal(root.get("reporterId"), reporterId));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        return reportRepository.findAll(spec, pageable(page, size, "createdAt")).map(this::toReportResponse);
    }

    @Transactional
    public void takedownProduct(Long productId) {
        takedownProduct(productId, "平台审核下架", null);
    }

    @Transactional
    public void takedownProduct(Long productId, String reason, Long adminId) {
        String normalizedReason = reason == null ? "" : reason.trim();
        if (normalizedReason.isBlank()) {
            throw new IllegalArgumentException("下架原因不能为空");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        if (product.getStatus() == Product.ProductStatus.SOLD) {
            throw new IllegalArgumentException("已售出商品不能强制下架");
        }
        product.setStatus(Product.ProductStatus.OFF);
        product.setTakedownReason(normalizedReason);
        product.setTakedownBy(adminId);
        product.setTakedownAt(LocalDateTime.now());
        productRepository.save(product);

        Notification notification = new Notification();
        notification.setUserId(product.getUserId());
        notification.setType("PRODUCT_TAKEDOWN");
        notification.setTitle("商品已被平台下架");
        notification.setContent("你的商品《" + product.getTitle() + "》已被平台下架，原因：" + normalizedReason);
        notificationRepository.save(notification);
    }

    public Page<OrderResponse> getOrders(int page, int size) {
        return getOrders(null, null, null, page, size);
    }

    public Page<OrderResponse> getOrders(String status, Long userId, Long productId, int page, int size) {
        Specification<Order> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            if (hasFilter(status)) {
                predicates.add(cb.equal(root.get("status"), Order.OrderStatus.valueOf(status.trim().toUpperCase(Locale.ROOT))));
            }
            if (userId != null) {
                predicates.add(cb.or(
                        cb.equal(root.get("buyerId"), userId),
                        cb.equal(root.get("sellerId"), userId)
                ));
            }
            if (productId != null) {
                predicates.add(cb.equal(root.get("productId"), productId));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        return orderRepository.findAll(spec, pageable(page, size, "createdAt"))
                .map(orderService::toResponse);
    }

    public Page<OrderResponse> getRefundingOrders(int page, int size) {
        return orderRepository.findByStatus(Order.OrderStatus.REFUNDING,
                PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(orderService::toResponse);
    }

    @Transactional
    public void handleReport(Long reportId, boolean takedown) {
        handleReport(reportId, takedown, null, null);
    }

    @Transactional
    public void handleReport(Long reportId, boolean takedown, String reason, Long adminId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("举报不存在"));
        String normalizedReason = reason == null ? "" : reason.trim();
        if (takedown) {
            String takedownReason = normalizedReason.isBlank() ? report.getReason() : normalizedReason;
            takedownProduct(report.getProductId(), takedownReason, adminId);
            report.setStatus(Report.ReportStatus.HANDLED);
        } else {
            report.setStatus(Report.ReportStatus.REJECTED);
        }
        report.setHandledReason(normalizedReason.isBlank() ? null : normalizedReason);
        report.setHandledBy(adminId);
        report.setHandledAt(LocalDateTime.now());
        reportRepository.save(report);
    }

    @Transactional
    public void approveRefund(Long orderId) {
        orderService.approveRefundByAdmin(orderId);
    }

    @Transactional
    public void rejectRefund(Long orderId, String reason) {
        orderService.rejectRefundByAdmin(orderId, reason);
    }

    public Map<String, Object> getStats() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("activeUsers", userRepository.countByStatus(User.UserStatus.ACTIVE));
        stats.put("bannedUsers", userRepository.countByStatus(User.UserStatus.BANNED));
        stats.put("totalProducts", productRepository.count());
        stats.put("onSaleProducts", productRepository.countByStatus(Product.ProductStatus.ON_SALE));
        stats.put("offProducts", productRepository.countByStatus(Product.ProductStatus.OFF));
        stats.put("soldProducts", productRepository.countByStatus(Product.ProductStatus.SOLD));
        stats.put("totalOrders", orderRepository.count());
        stats.put("refundingOrders", orderRepository.countByStatus(Order.OrderStatus.REFUNDING));
        stats.put("doneOrders", orderRepository.countByStatus(Order.OrderStatus.DONE));
        stats.put("todayOrders", orderRepository.countByCreatedAtAfter(todayStart));
        stats.put("totalAmount", orderRepository.sumFinalPrice());
        stats.put("todayAmount", orderRepository.sumDonePriceAfter(todayStart));
        stats.put("pendingReports", reportRepository.countByStatus(Report.ReportStatus.PENDING));
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
        response.setMuted(Boolean.TRUE.equals(user.getMuted()));
        response.setPublishBanned(Boolean.TRUE.equals(user.getPublishBanned()));
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
        productImageRepository.findByProductIdOrderBySortOrder(report.getProductId()).stream()
                .findFirst()
                .ifPresent(image -> response.setProductImageUrl(fileService.toAbsoluteUrl(image.getImageUrl())));
        response.setReason(report.getReason());
        if (report.getEvidenceImageUrls() != null && !report.getEvidenceImageUrls().isBlank()) {
            response.setEvidenceImageUrls(Arrays.stream(report.getEvidenceImageUrls().split(","))
                    .filter(url -> !url.isBlank())
                    .toList());
        }
        response.setStatus(report.getStatus().name());
        response.setHandledReason(report.getHandledReason());
        response.setHandledBy(report.getHandledBy());
        response.setHandledAt(report.getHandledAt());
        response.setCreatedAt(report.getCreatedAt());
        return response;
    }

    @Transactional
    public void sendAnnouncement(String content) {
        sendAnnouncement("系统公告", content);
    }

    @Transactional
    public void sendAnnouncement(String title, String content) {
        String normalizedTitle = title == null || title.isBlank() ? "系统公告" : title.trim();
        String normalizedContent = content == null ? "" : content.trim();
        if (normalizedContent.isBlank()) {
            throw new IllegalArgumentException("公告内容不能为空");
        }
        // 给所有用户创建系统通知
        userRepository.findAll().forEach(user -> {
            Notification n = new Notification();
            n.setUserId(user.getId());
            n.setType("SYSTEM");
            n.setTitle(normalizedTitle);
            n.setContent(normalizedContent);
            notificationRepository.save(n);
        });
    }

    private Pageable pageable(int page, int size, String sortProperty) {
        return pageable(page, size, sortProperty, "desc");
    }

    private Pageable pageable(int page, int size, String sortProperty, String direction) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Sort sort = "asc".equalsIgnoreCase(direction)
                ? Sort.by(sortProperty).ascending()
                : Sort.by(sortProperty).descending();
        return PageRequest.of(safePage, safeSize, sort);
    }

    private String normalizeUserSort(String sort) {
        if (sort == null || sort.isBlank()) return "createdAt";
        return switch (sort.trim()) {
            case "nickname", "phone", "studentId", "creditScore", "status", "createdAt" -> sort.trim();
            default -> "createdAt";
        };
    }

    private boolean hasFilter(String value) {
        return value != null && !value.isBlank() && !"ALL".equalsIgnoreCase(value.trim());
    }

    private User getMutableUser(Long userId, String adminMessage) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        if (user.getRole() == User.Role.ADMIN) {
            throw new IllegalArgumentException(adminMessage);
        }
        return user;
    }
}
