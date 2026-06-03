package com.tps.service;

/**
 * 文件说明：业务服务层，负责封装核心业务规则、事务与对象组装。
 */

import com.tps.dto.order.OrderResponse;
import com.tps.dto.order.ReviewResponse;
import com.tps.entity.*;
import com.tps.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final NotificationRepository notificationRepository;
    private final FileService fileService;

    @Transactional
    public OrderResponse createOrder(Long buyerId, Long productId, BigDecimal finalPrice) {
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        if (product.getStatus() != Product.ProductStatus.ON_SALE)
            throw new IllegalArgumentException("商品已下架或已售出");
        if (product.getUserId().equals(buyerId))
            throw new IllegalArgumentException("不能购买自己的商品");
        if (finalPrice == null || finalPrice.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("订单价格不正确");
        if (orderRepository.existsByProductIdAndStatusIn(productId, activeStatuses()))
            throw new IllegalArgumentException("该商品已有进行中的订单");

        Order order = new Order();
        order.setProductId(productId);
        order.setBuyerId(buyerId);
        order.setSellerId(product.getUserId());
        order.setPrice(finalPrice);
        order.setStatus(Order.OrderStatus.PENDING);
        product.setStatus(Product.ProductStatus.OFF);
        productRepository.save(product);
        Order saved = orderRepository.save(order);
        notifyUser(product.getUserId(), "ORDER", "新订单", "你的商品有新的待支付订单");
        return toResponse(saved);
    }

    public Page<OrderResponse> myOrders(Long userId, String role, int page, int size) {
        return myOrders(userId, role, null, page, size);
    }

    public Page<OrderResponse> myOrders(Long userId, String role, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Order.OrderStatus orderStatus = null;
        if (status != null && !status.isBlank()) {
            orderStatus = Order.OrderStatus.valueOf(status);
        }
        if ("seller".equals(role)) {
            return orderStatus == null
                    ? orderRepository.findBySellerId(userId, pageable).map(this::toResponse)
                    : orderRepository.findBySellerIdAndStatus(userId, orderStatus, pageable).map(this::toResponse);
        }
        if (orderStatus != null) return orderRepository.findByBuyerIdAndStatus(userId, orderStatus, pageable).map(this::toResponse);
        return orderRepository.findByBuyerId(userId, pageable).map(this::toResponse);
    }

    public OrderResponse getOrder(Long orderId, Long userId) {
        return toResponse(getOwnedOrder(orderId, userId));
    }

    public Page<ReviewResponse> getReviewsForUser(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return reviewRepository.findByRevieweeIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toReviewResponse);
    }

    private Order getOwnedOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId))
            throw new IllegalArgumentException("无权限查看此订单");
        return order;
    }

    @Transactional
    public void pay(Long orderId, Long userId) {
        Order order = getOwnedOrder(orderId, userId);
        if (!order.getBuyerId().equals(userId)) throw new IllegalArgumentException("只有买家可以支付");
        if (order.getStatus() != Order.OrderStatus.PENDING) throw new IllegalArgumentException("订单状态不正确");
        order.setStatus(Order.OrderStatus.PAID);
        orderRepository.save(order);
        notifyUser(order.getSellerId(), "ORDER", "订单已支付", "买家已完成付款，请及时发货");
    }

    @Transactional
    public void ship(Long orderId, Long userId, String trackingNumber) {
        Order order = getOwnedOrder(orderId, userId);
        if (!order.getSellerId().equals(userId)) throw new IllegalArgumentException("只有卖家可以发货");
        if (order.getStatus() != Order.OrderStatus.PAID) throw new IllegalArgumentException("订单未支付");
        order.setTrackingNumber(trackingNumber);
        order.setStatus(Order.OrderStatus.SHIPPED);
        orderRepository.save(order);
        notifyUser(order.getBuyerId(), "ORDER", "卖家已发货", "卖家已发货，请注意查收");
    }

    @Transactional
    public void confirm(Long orderId, Long userId) {
        Order order = getOwnedOrder(orderId, userId);
        if (!order.getBuyerId().equals(userId)) throw new IllegalArgumentException("只有买家可以确认收货");
        if (order.getStatus() != Order.OrderStatus.SHIPPED) throw new IllegalArgumentException("订单未发货");
        order.setStatus(Order.OrderStatus.DONE);
        // 将商品标记为已售出
        productRepository.findById(order.getProductId()).ifPresent(p -> {
            p.setStatus(Product.ProductStatus.SOLD);
            productRepository.save(p);
        });
        orderRepository.save(order);
        notifyUser(order.getSellerId(), "ORDER", "订单已完成", "买家已确认收货");
    }

    @Transactional
    public void cancel(Long orderId, Long userId) {
        Order order = getOwnedOrder(orderId, userId);
        if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId))
            throw new IllegalArgumentException("无权限取消此订单");
        if (order.getStatus() == Order.OrderStatus.DONE)
            throw new IllegalArgumentException("已完成的订单不能取消");
        if (order.getStatus() == Order.OrderStatus.SHIPPED)
            throw new IllegalArgumentException("已发货订单不能直接取消");
        order.setStatus(Order.OrderStatus.CANCELLED);
        productRepository.findById(order.getProductId()).ifPresent(p -> {
            if (p.getStatus() == Product.ProductStatus.OFF) {
                p.setStatus(Product.ProductStatus.ON_SALE);
                productRepository.save(p);
            }
        });
        orderRepository.save(order);
        Long targetUserId = order.getBuyerId().equals(userId) ? order.getSellerId() : order.getBuyerId();
        notifyUser(targetUserId, "ORDER", "订单已取消", "订单已被取消");
    }

    private List<Order.OrderStatus> activeStatuses() {
        return List.of(Order.OrderStatus.PENDING, Order.OrderStatus.PAID, Order.OrderStatus.SHIPPED);
    }

    @Transactional
    public ReviewResponse review(Long orderId, Long userId, Integer score, String content) {
        if (score == null || score < 1 || score > 5) {
            throw new IllegalArgumentException("评分必须为1-5");
        }
        Order order = getOwnedOrder(orderId, userId);
        if (order.getStatus() != Order.OrderStatus.DONE) {
            throw new IllegalArgumentException("订单完成后才能评价");
        }
        if (reviewRepository.existsByOrderIdAndReviewerId(orderId, userId)) {
            throw new IllegalArgumentException("不能重复评价");
        }
        Long revieweeId = order.getBuyerId().equals(userId) ? order.getSellerId() : order.getBuyerId();
        Review review = new Review();
        review.setOrderId(orderId);
        review.setReviewerId(userId);
        review.setRevieweeId(revieweeId);
        review.setScore(score);
        review.setContent(content);
        Review saved = reviewRepository.save(review);
        updateCreditScore(revieweeId);
        notifyUser(revieweeId, "ORDER", "收到新评价", "你收到了一条新的交易评价");
        return toReviewResponse(saved);
    }

    @Transactional
    public void requestRefund(Long orderId, Long userId, String reason) {
        Order order = getOwnedOrder(orderId, userId);
        if (!order.getBuyerId().equals(userId)) {
            throw new IllegalArgumentException("只有买家可以申请退款");
        }
        if (order.getStatus() != Order.OrderStatus.PAID && order.getStatus() != Order.OrderStatus.SHIPPED) {
            throw new IllegalArgumentException("当前订单状态不能申请退款");
        }
        order.setStatus(Order.OrderStatus.REFUNDING);
        order.setRemark(reason);
        orderRepository.save(order);
        notifyUser(order.getSellerId(), "REFUND", "买家申请退款", "买家申请退款，请处理");
    }

    @Transactional
    public void approveRefund(Long orderId, Long userId) {
        Order order = getOwnedOrder(orderId, userId);
        if (!order.getSellerId().equals(userId)) {
            throw new IllegalArgumentException("只有卖家可以同意退款");
        }
        approveRefundInternal(order);
    }

    @Transactional
    public void approveRefundByAdmin(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        approveRefundInternal(order);
    }

    @Transactional
    public void rejectRefundByAdmin(Long orderId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        if (order.getStatus() != Order.OrderStatus.REFUNDING) {
            throw new IllegalArgumentException("订单不在退款中");
        }
        order.setStatus(order.getTrackingNumber() == null || order.getTrackingNumber().isBlank()
                ? Order.OrderStatus.PAID
                : Order.OrderStatus.SHIPPED);
        String normalizedReason = reason == null || reason.isBlank() ? "平台审核未通过退款申请" : reason.trim();
        order.setRemark(normalizedReason);
        orderRepository.save(order);
        notifyUser(order.getBuyerId(), "REFUND", "退款申请未通过", normalizedReason);
        notifyUser(order.getSellerId(), "REFUND", "退款申请已驳回", "平台已驳回该订单的退款申请");
    }

    private void approveRefundInternal(Order order) {
        if (order.getStatus() != Order.OrderStatus.REFUNDING) {
            throw new IllegalArgumentException("订单不在退款中");
        }
        order.setStatus(Order.OrderStatus.REFUNDED);
        productRepository.findById(order.getProductId()).ifPresent(product -> {
            product.setStatus(Product.ProductStatus.ON_SALE);
            productRepository.save(product);
        });
        orderRepository.save(order);
        notifyUser(order.getBuyerId(), "REFUND", "退款已通过", "退款申请已通过");
    }

    private void updateCreditScore(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            List<Review> reviews = reviewRepository.findByRevieweeId(userId);
            if (!reviews.isEmpty()) {
                double average = reviews.stream().mapToInt(Review::getScore).average().orElse(5.0);
                user.setCreditScore((int) Math.round(average * 20));
                userRepository.save(user);
            }
        });
    }

    private void notifyUser(Long userId, String type, String title, String content) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notificationRepository.save(notification);
    }

    public OrderResponse toResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setProductId(order.getProductId());
        productRepository.findById(order.getProductId()).ifPresent(product -> {
            response.setProductTitle(product.getTitle());
            productImageRepository.findByProductIdOrderBySortOrder(product.getId()).stream()
                    .findFirst()
                    .ifPresent(image -> response.setProductCover(fileService.toAbsoluteUrl(image.getImageUrl())));
        });
        response.setBuyerId(order.getBuyerId());
        userRepository.findById(order.getBuyerId()).ifPresent(user -> response.setBuyerNickname(user.getNickname()));
        response.setSellerId(order.getSellerId());
        userRepository.findById(order.getSellerId()).ifPresent(user -> response.setSellerNickname(user.getNickname()));
        response.setPrice(order.getPrice());
        response.setStatus(order.getStatus().name());
        response.setRemark(order.getRemark());
        response.setTrackingNumber(order.getTrackingNumber());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());
        return response;
    }

    private ReviewResponse toReviewResponse(Review review) {
        ReviewResponse response = new ReviewResponse();
        response.setId(review.getId());
        response.setOrderId(review.getOrderId());
        response.setReviewerId(review.getReviewerId());
        response.setRevieweeId(review.getRevieweeId());
        response.setScore(review.getScore());
        response.setContent(review.getContent());
        response.setCreatedAt(review.getCreatedAt());
        return response;
    }
}
