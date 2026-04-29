package com.tps.repository;

import com.tps.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {
    Page<Report> findByStatus(Report.ReportStatus status, Pageable pageable);
    boolean existsByReporterIdAndProductId(Long reporterId, Long productId);
}
