package com.tps.repository;

import com.tps.entity.Favorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    Optional<Favorite> findByUserIdAndProductId(Long userId, Long productId);
    boolean existsByUserIdAndProductId(Long userId, Long productId);
    List<Favorite> findByUserId(Long userId);
    Page<Favorite> findByUserId(Long userId, Pageable pageable);
    void deleteByUserIdAndProductId(Long userId, Long productId);
}
