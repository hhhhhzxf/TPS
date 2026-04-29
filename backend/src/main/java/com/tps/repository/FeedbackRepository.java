package com.tps.repository;

import com.tps.entity.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    Page<Feedback> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Page<Feedback> findByStatusOrderByCreatedAtDesc(Feedback.FeedbackStatus status, Pageable pageable);
    Page<Feedback> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
