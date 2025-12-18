package com.loopers.infrastructure.metrics;

import com.loopers.domain.metrics.ProductMetrics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductMetricsJpaRepository extends JpaRepository<ProductMetrics, Long> {

  @Modifying
  @Query(
      value =
          """
          INSERT INTO product_metrics (ref_product_id, like_count, sales_count, view_count, updated_at)
          VALUES (:productId, GREATEST(:delta, 0), 0, 0, :occurredAt)
          ON DUPLICATE KEY UPDATE
            like_count = GREATEST(like_count + :delta, 0),
            updated_at = GREATEST(updated_at, :occurredAt)
          """,
      nativeQuery = true)
  void upsertLikeCount(
      @Param("productId") Long productId,
      @Param("delta") int delta,
      @Param("occurredAt") Long occurredAt);

  @Modifying
  @Query(
      value =
          """
          INSERT INTO product_metrics (ref_product_id, like_count, sales_count, view_count, updated_at)
          VALUES (:productId, 0, :quantity, 0, :occurredAt)
          ON DUPLICATE KEY UPDATE
            sales_count = sales_count + :quantity,
            updated_at = GREATEST(updated_at, :occurredAt)
          """,
      nativeQuery = true)
  void upsertSalesCount(
      @Param("productId") Long productId,
      @Param("quantity") int quantity,
      @Param("occurredAt") Long occurredAt);

  @Modifying
  @Query(
      value =
          """
          INSERT INTO product_metrics (ref_product_id, like_count, sales_count, view_count, updated_at)
          VALUES (:productId, 0, 0, :count, :occurredAt)
          ON DUPLICATE KEY UPDATE
            view_count = view_count + :count,
            updated_at = GREATEST(updated_at, :occurredAt)
          """,
      nativeQuery = true)
  void upsertViewCount(
      @Param("productId") Long productId,
      @Param("count") int count,
      @Param("occurredAt") Long occurredAt);
}
