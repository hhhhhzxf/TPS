package com.tps.repository;

import com.tps.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Optional<Conversation> findByProductIdAndBuyerIdAndSellerId(Long productId, Long buyerId, Long sellerId);

    Page<Conversation> findByBuyerIdOrSellerIdOrderByUpdatedAtDesc(Long buyerId, Long sellerId, Pageable pageable);

    // 兼容旧查询
    @Query("SELECT c FROM Conversation c WHERE c.buyerId = :userId OR c.sellerId = :userId ORDER BY c.updatedAt DESC")
    java.util.List<Conversation> findByUserId(@Param("userId") Long userId);
}
