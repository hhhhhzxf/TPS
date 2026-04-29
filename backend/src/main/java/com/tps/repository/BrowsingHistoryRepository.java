package com.tps.repository;

import com.tps.entity.BrowsingHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BrowsingHistoryRepository extends JpaRepository<BrowsingHistory, Long> {
    Optional<BrowsingHistory> findByUserIdAndProductId(Long userId, Long productId);
    Page<BrowsingHistory> findByUserIdOrderByViewedAtDesc(Long userId, Pageable pageable);
    void deleteByUserId(Long userId);
    void deleteByUserIdAndProductId(Long userId, Long productId);
}
