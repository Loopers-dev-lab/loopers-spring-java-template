package com.loopers.core.infra.database.mysql.product;

import com.loopers.core.domain.product.vo.ProductMetricAggregation;
import com.loopers.core.infra.database.mysql.product.entity.DailyProductMetricEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DailyProductMetricJpaRepository extends JpaRepository<DailyProductMetricEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select pme from DailyProductMetricEntity pme where pme.productId = :productId and CAST(pme.createdAt AS date) = CAST(:createdAt AS date)")
    Optional<DailyProductMetricEntity> findByProductIdWithLock(Long productId, LocalDateTime createdAt);

    @Query("select count(distinct pme.productId) from DailyProductMetricEntity pme " +
            "where cast(pme.createdAt as date) >= :startDate " +
            "and cast(pme.createdAt as date) <= :endDate")
    Long countDistinctProductIds(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("select new com.loopers.core.domain.product.vo.ProductMetricAggregation(" +
            "cast(pme.productId as string), " +
            "sum(pme.likeCount), " +
            "sum(pme.viewCount), " +
            "sum(pme.totalSalesCount)) " +
            "from DailyProductMetricEntity pme " +
            "where cast(pme.createdAt as date) >= :startDate " +
            "and cast(pme.createdAt as date) <= :endDate " +
            "group by pme.productId " +
            "order by pme.productId " +
            "limit :partitionLimit offset :partitionOffset")
    List<ProductMetricAggregation> findAggregatedBy(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("partitionOffset") long partitionOffset,
            @Param("partitionLimit") long partitionLimit
    );
}
