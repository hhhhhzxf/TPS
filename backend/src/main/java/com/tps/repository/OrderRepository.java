package com.tps.repository;

/**
 * 文件说明：数据访问层，负责声明实体查询与持久化接口。
 */

import com.tps.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Collection;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
    Page<Order> findByBuyerId(Long buyerId, Pageable pageable);
    Page<Order> findBySellerId(Long sellerId, Pageable pageable);
    Page<Order> findByBuyerIdAndStatus(Long buyerId, Order.OrderStatus status, Pageable pageable);
    Page<Order> findBySellerIdAndStatus(Long sellerId, Order.OrderStatus status, Pageable pageable);
    Page<Order> findByStatus(Order.OrderStatus status, Pageable pageable);
    List<Order> findByProductId(Long productId);
    boolean existsByProductIdAndStatusIn(Long productId, Collection<Order.OrderStatus> statuses);
    long countByStatus(Order.OrderStatus status);
    long countByCreatedAtAfter(LocalDateTime createdAt);

    @Query(value = "SELECT COALESCE(SUM(price), 0) FROM orders WHERE status = 'DONE'", nativeQuery = true)
    BigDecimal sumFinalPrice();

    @Query(value = "SELECT COALESCE(SUM(price), 0) FROM orders WHERE status = 'DONE' AND created_at >= :createdAt", nativeQuery = true)
    BigDecimal sumDonePriceAfter(LocalDateTime createdAt);
}
