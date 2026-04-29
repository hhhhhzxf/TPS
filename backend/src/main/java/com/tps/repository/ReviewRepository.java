package com.tps.repository;

import com.tps.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByRevieweeId(Long revieweeId);
    boolean existsByOrderIdAndReviewerId(Long orderId, Long reviewerId);
}
