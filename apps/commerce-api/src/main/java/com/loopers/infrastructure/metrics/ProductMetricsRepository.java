package com.loopers.infrastructure.metrics;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProductMetricsRepository extends JpaRepository<ProductMetrics, Long> {

    Optional<ProductMetrics> findByProductId(Long productId);

    Optional<ProductMetrics> findByProductIdAndDate(Long productId, LocalDate date);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pm FROM ProductMetrics pm WHERE pm.productId = :productId AND pm.date = :date")
    Optional<ProductMetrics> findByProductIdAndDateForUpdate(
            @Param("productId") Long productId,
            @Param("date") LocalDate date);

    List<ProductMetrics> findByDateBetween(LocalDate startDate, LocalDate endDate);

    @Modifying
    @Query(value = """
            INSERT INTO product_metrics (product_id, date, view_count, like_count, order_count, total_quantity)
            VALUES (:productId, :date, :viewCount, 0, 0, 0)
            ON DUPLICATE KEY UPDATE view_count = view_count + :viewCount
            """, nativeQuery = true)
    void upsertViewCount(@Param("productId") Long productId,
                         @Param("date") LocalDate date,
                         @Param("viewCount") int viewCount);

    @Modifying
    @Query(value = """
            INSERT INTO product_metrics (product_id, date, view_count, like_count, order_count, total_quantity)
            VALUES (:productId, :date, 0, :likeCount, 0, 0)
            ON DUPLICATE KEY UPDATE like_count = like_count + :likeCount
            """, nativeQuery = true)
    void upsertLikeCount(@Param("productId") Long productId,
                         @Param("date") LocalDate date,
                         @Param("likeCount") int likeCount);

    @Modifying
    @Query(value = """
            INSERT INTO product_metrics (product_id, date, view_count, like_count, order_count, total_quantity)
            VALUES (:productId, :date, 0, 0, 1, :quantity)
            ON DUPLICATE KEY UPDATE order_count = order_count + 1, total_quantity = total_quantity + :quantity
            """, nativeQuery = true)
    void upsertOrder(@Param("productId") Long productId,
                     @Param("date") LocalDate date,
                     @Param("quantity") int quantity);
}
