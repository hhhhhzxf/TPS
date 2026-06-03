package com.tps.repository;

/**
 * 文件说明：数据访问层，负责声明实体查询与持久化接口。
 */

import com.tps.entity.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ReportRepository extends JpaRepository<Report, Long>, JpaSpecificationExecutor<Report> {
    Page<Report> findByStatus(Report.ReportStatus status, Pageable pageable);
    boolean existsByReporterIdAndProductId(Long reporterId, Long productId);
    long countByStatus(Report.ReportStatus status);
}
