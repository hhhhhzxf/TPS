package com.tps.repository;

/**
 * 文件说明：数据访问层，负责声明实体查询与持久化接口。
 */

import com.tps.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
    Page<Product> findByStatus(Product.ProductStatus status, Pageable pageable);
    List<Product> findByUserIdAndStatus(Long userId, Product.ProductStatus status);
    List<Product> findByUserId(Long userId);
    long countByStatus(Product.ProductStatus status);

    @Modifying
    @Query("UPDATE Product p SET p.viewCount = p.viewCount + 1 WHERE p.id = :id")
    void incrementViewCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Product p SET p.favoriteCount = p.favoriteCount + 1 WHERE p.id = :id")
    void incrementFavoriteCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Product p SET p.favoriteCount = CASE WHEN p.favoriteCount > 0 THEN p.favoriteCount - 1 ELSE 0 END WHERE p.id = :id")
    void decrementFavoriteCount(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);
}
