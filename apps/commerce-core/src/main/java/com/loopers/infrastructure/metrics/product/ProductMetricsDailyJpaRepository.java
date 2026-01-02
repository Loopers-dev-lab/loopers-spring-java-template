package com.loopers.infrastructure.metrics.product;

import com.loopers.domain.metrics.product.ProductMetricsDaily;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ProductMetricsDailyJpaRepository extends JpaRepository<ProductMetricsDaily, Long> {
    
    Optional<ProductMetricsDaily> findByProductIdAndDate(Long productId, LocalDate date);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pmd FROM ProductMetricsDaily pmd WHERE pmd.productId = :productId AND pmd.date = :date")
    Optional<ProductMetricsDaily> findByProductIdAndDateForUpdate(
        @Param("productId") Long productId, 
        @Param("date") LocalDate date
    );
    
    List<ProductMetricsDaily> findByDateBetween(LocalDate startDate, LocalDate endDate);
    
    /**
     * 특정 날짜 범위의 메트릭을 product_id별로 집계
     */
    @Query(
        "SELECT " +
        "  pmd.productId as productId, " +
        "  SUM(pmd.likeCount) as totalLikeCount, " +
        "  SUM(pmd.viewCount) as totalViewCount, " +
        "  SUM(pmd.soldCount) as totalSoldCount " +
        "FROM ProductMetricsDaily pmd " +
        "WHERE pmd.date >= :startDate AND pmd.date <= :endDate " +
        "GROUP BY pmd.productId "
    )
    List<Object[]> findAggregatedByDateBetween(
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    /**
     * 특정 날짜 범위의 메트릭을 product_id별로 집계하여 페이징 조회 (배치용)
     *
     * @param startDate 시작일
     * @param endDate 종료일
     * @param pageable 페이징 정보
     * @return 집계된 메트릭 페이지
     */
    @Query(
            value =
                    "SELECT " +
                            "  pmd.product_id as productId, " +
                            "  SUM(pmd.like_count) as totalLikeCount, " +
                            "  SUM(pmd.view_count) as totalViewCount, " +
                            "  SUM(pmd.sold_count) as totalSoldCount " +
                            "FROM tb_product_metrics_daily pmd " +
                            "WHERE pmd.date >= :startDate AND pmd.date <= :endDate " +
                            "GROUP BY pmd.product_id " +
                            "ORDER BY pmd.product_id",
            countQuery =
                    "SELECT COUNT(DISTINCT pmd.product_id) " +
                            "FROM tb_product_metrics_daily pmd " +
                            "WHERE pmd.date >= :startDate AND pmd.date <= :endDate",
            nativeQuery = true
    )
    Page<Object[]> findAggregatedByDateBetweenPaged(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );
}


