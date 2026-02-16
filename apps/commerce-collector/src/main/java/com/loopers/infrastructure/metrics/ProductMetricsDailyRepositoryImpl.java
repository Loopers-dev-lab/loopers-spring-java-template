package com.loopers.infrastructure.metrics;

import com.loopers.domain.metrics.ProductMetricsDaily;
import com.loopers.domain.metrics.ProductMetricsDailyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductMetricsDailyRepositoryImpl implements ProductMetricsDailyRepository {

    private final ProductMetricsDailyJpaRepository productMetricsDailyJpaRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Optional<ProductMetricsDaily> findByProductIdAndMetricDate(Long productId, LocalDate metricDate) {
        return productMetricsDailyJpaRepository.findByProductIdAndMetricDate(productId, metricDate);
    }

    @Override
    public List<ProductMetricsDaily> findAllByMetricDateAndIsProcessed(LocalDate metricDate, boolean isProcessed) {
        return productMetricsDailyJpaRepository.findAllByMetricDateAndIsProcessed(metricDate, isProcessed);
    }

    @Override
    public ProductMetricsDaily save(ProductMetricsDaily daily) {
        return productMetricsDailyJpaRepository.save(daily);
    }

    @Override
    public void saveAll(List<ProductMetricsDaily> unprocessedRecords) {
        productMetricsDailyJpaRepository.saveAll(unprocessedRecords);
    }

    @Override
    public void upsertLikeDeltas(Map<Long, Integer> likeDeltas, LocalDate metricDate) {
        if (likeDeltas.isEmpty()) {
            return;
        }

        String sql = """
            INSERT INTO product_metrics_daily
                (product_id, metric_date, like_delta, view_delta, order_delta, is_processed, created_at, updated_at)
            VALUES (?, ?, ?, 0, 0, FALSE, NOW(), NOW())
            ON DUPLICATE KEY UPDATE
                like_delta = like_delta + VALUES(like_delta),
                updated_at = NOW()
            """;

        List<Map.Entry<Long, Integer>> entries = new ArrayList<>(likeDeltas.entrySet());

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Map.Entry<Long, Integer> entry = entries.get(i);
                ps.setLong(1, entry.getKey());
                ps.setDate(2, Date.valueOf(metricDate));
                ps.setInt(3, entry.getValue());
            }

            @Override
            public int getBatchSize() {
                return entries.size();
            }
        });

        log.info("좋아요 증감 Upsert 완료 - {} 건, 일자: {}", entries.size(), metricDate);
    }

    @Override
    public void upsertViewDeltas(Map<Long, Integer> viewDeltas, LocalDate metricDate) {
        if (viewDeltas.isEmpty()) {
            return;
        }

        String sql = """
            INSERT INTO product_metrics_daily
                (product_id, metric_date, like_delta, view_delta, order_delta, is_processed, created_at, updated_at)
            VALUES (?, ?, 0, ?, 0, FALSE, NOW(), NOW())
            ON DUPLICATE KEY UPDATE
                view_delta = view_delta + VALUES(view_delta),
                updated_at = NOW()
            """;

        List<Map.Entry<Long, Integer>> entries = new ArrayList<>(viewDeltas.entrySet());

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Map.Entry<Long, Integer> entry = entries.get(i);
                ps.setLong(1, entry.getKey());
                ps.setDate(2, Date.valueOf(metricDate));
                ps.setInt(3, entry.getValue());
            }

            @Override
            public int getBatchSize() {
                return entries.size();
            }
        });

        log.info("조회수 증감 Upsert 완료 - {} 건, 일자: {}", entries.size(), metricDate);
    }

    @Override
    public void upsertOrderDeltas(Map<Long, com.loopers.application.order.OrderMetrics> orderMetrics, LocalDate metricDate) {
        if (orderMetrics.isEmpty()) {
            return;
        }

        String sql = """
            INSERT INTO product_metrics_daily
                (product_id, metric_date, like_delta, view_delta, order_delta, is_processed, created_at, updated_at)
            VALUES (?, ?, 0, 0, ?, FALSE, NOW(), NOW())
            ON DUPLICATE KEY UPDATE
                order_delta = order_delta + VALUES(order_delta),
                updated_at = NOW()
            """;

        List<Map.Entry<Long, com.loopers.application.order.OrderMetrics>> entries = new ArrayList<>(orderMetrics.entrySet());

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Map.Entry<Long, com.loopers.application.order.OrderMetrics> entry = entries.get(i);
                ps.setLong(1, entry.getKey());
                ps.setDate(2, Date.valueOf(metricDate));
                ps.setInt(3, entry.getValue().getTotalQuantity());  // order_delta는 총 수량을 저장
            }

            @Override
            public int getBatchSize() {
                return entries.size();
            }
        });

        log.info("주문 증감 Upsert 완료 - {} 건, 일자: {} (수량 기준)", entries.size(), metricDate);
    }

    @Override
    public int deleteByMetricDateBefore(LocalDate cutoffDate) {
        return productMetricsDailyJpaRepository.deleteByMetricDateBefore(cutoffDate);
    }

    @Override
    public List<ProductMetricsDaily> findAllByMetricDateBetween(LocalDate startDate, LocalDate endDate) {
        return productMetricsDailyJpaRepository
                .findAllByMetricDateBetween(startDate, endDate);
    }

    @Override
    public org.springframework.data.domain.Page<com.loopers.domain.metrics.dto.WeeklyAggregationDto> findWeeklyAggregation(
            Integer year,
            Integer week,
            LocalDate startDate,
            LocalDate endDate,
            org.springframework.data.domain.Pageable pageable
    ) {
        return productMetricsDailyJpaRepository.findWeeklyAggregation(
                year, week, startDate, endDate, pageable
        );
    }

    @Override
    public org.springframework.data.domain.Page<com.loopers.domain.metrics.dto.MonthlyAggregationDto> findMonthlyAggregation(
            Integer year,
            Integer month,
            LocalDate startDate,
            LocalDate endDate,
            org.springframework.data.domain.Pageable pageable
    ) {
        return productMetricsDailyJpaRepository.findMonthlyAggregation(
                year, month, startDate, endDate, pageable
        );
    }
}
