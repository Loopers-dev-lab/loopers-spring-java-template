package com.loopers.infrastructure.productmetrics;

import com.loopers.domain.productmetrics.ProductMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductMetricsJpaRepository extends JpaRepository<ProductMetrics, Long> {

    Optional<ProductMetrics> findByProductId(Long productId);

    @Modifying
    @Query(value = """
        INSERT INTO product_metrics (product_id, like_count, sales_count, view_count, created_at, updated_at)
        VALUES (:productId, 1, 0, 0, NOW(), NOW())
        ON DUPLICATE KEY UPDATE
            like_count = like_count + 1,
            updated_at = NOW()
        """,
        nativeQuery = true
    )
    void upsertLikeCount(@Param("productId") Long productId);

    @Modifying
    @Query(value = """
        INSERT INTO product_metrics (product_id, like_count, sales_count, view_count, created_at, updated_at)
        VALUES (:productId, 0, 0, 0, NOW(), NOW())
        ON DUPLICATE KEY UPDATE
            like_count = CASE WHEN like_count > 0 THEN like_count - 1 ELSE 0 END,
            updated_at = NOW()
        """,
        nativeQuery = true
    )
    void upsertUnlikeCount(@Param("productId") Long productId);

    @Modifying
    @Query(value = """
        INSERT INTO product_metrics (product_id, like_count, sales_count, view_count, created_at, updated_at)
        VALUES (:productId, 0, :quantity, 0, NOW(), NOW())
        ON DUPLICATE KEY UPDATE
            sales_count = sales_count + :quantity,
            updated_at = NOW()
        """,
        nativeQuery = true
    )
    void upsertSalesCount(@Param("productId") Long productId, @Param("quantity") int quantity);

    @Modifying
    @Query(value = """
        INSERT INTO product_metrics (product_id, like_count, sales_count, view_count, created_at, updated_at)
        VALUES (:productId, 0, 0, 1, NOW(), NOW())
        ON DUPLICATE KEY UPDATE
            view_count = view_count + 1,
            updated_at = NOW()
        """,
        nativeQuery = true
    )
    void upsertViewCount(@Param("productId") Long productId);
}
