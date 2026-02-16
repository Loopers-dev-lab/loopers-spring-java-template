package com.loopers.infrastructure.metrics;

import com.loopers.domain.metrics.ProductMetricsMonthly;
import com.loopers.domain.metrics.ProductMetricsMonthlyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductMetricsMonthlyRepositoryImpl implements ProductMetricsMonthlyRepository {

    private final ProductMetricsMonthlyJpaRepository monthlyJpaRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public ProductMetricsMonthly save(ProductMetricsMonthly metrics) {
        return monthlyJpaRepository.save(metrics);
    }

    @Override
    public void saveAll(List<ProductMetricsMonthly> metricsList) {
        if (metricsList.isEmpty()) {
            return;
        }

        // UPSERT를 위한 Bulk Insert with ON DUPLICATE KEY UPDATE
        String sql = """
            INSERT INTO mv_product_metrics_monthly
                (product_id, year, month, period_start_date, period_end_date,
                 total_like_count, total_view_count, total_order_count,
                 aggregated_at, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
            ON DUPLICATE KEY UPDATE
                total_like_count = VALUES(total_like_count),
                total_view_count = VALUES(total_view_count),
                total_order_count = VALUES(total_order_count),
                aggregated_at = VALUES(aggregated_at),
                updated_at = NOW()
        """;

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ProductMetricsMonthly metrics = metricsList.get(i);
                ps.setLong(1, metrics.getProductId());
                ps.setInt(2, metrics.getYear());
                ps.setInt(3, metrics.getMonth());
                ps.setDate(4, Date.valueOf(metrics.getPeriodStartDate()));
                ps.setDate(5, Date.valueOf(metrics.getPeriodEndDate()));
                ps.setLong(6, metrics.getTotalLikeCount());
                ps.setLong(7, metrics.getTotalViewCount());
                ps.setLong(8, metrics.getTotalOrderCount());
                ps.setTimestamp(9, metrics.getAggregatedAt() != null
                        ? Timestamp.from(metrics.getAggregatedAt().toInstant())
                        : new Timestamp(System.currentTimeMillis()));
            }

            @Override
            public int getBatchSize() {
                return metricsList.size();
            }
        });

        log.info("월간 집계 Bulk UPSERT 완료: {} 건", metricsList.size());
    }

    @Override
    public int deleteByYearAndMonthBefore(Integer year, Integer month) {
        return monthlyJpaRepository.deleteByYearAndMonthBefore(year, month);
    }

    @Override
    public List<ProductMetricsMonthly> findAll() {
        return monthlyJpaRepository.findAll();
    }
}
