package com.loopers.infrastructure.metrics;

import com.loopers.domain.metrics.ProductMetrics;
import com.loopers.domain.metrics.ProductMetricsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductMetricsRepositoryImpl implements ProductMetricsRepository {

    private final ProductMetricsJpaRepository productMetricsJpaRepository;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Optional<ProductMetrics> findByProductId(Long productId) {
        return productMetricsJpaRepository.findByProductId(productId);
    }

    @Override
    public Optional<ProductMetrics> findByProductIdWithLock(Long productId) {
        return productMetricsJpaRepository.findByProductIdWithLock(productId);
    }

    @Override
    public ProductMetrics save(ProductMetrics productMetrics) {
        return productMetricsJpaRepository.save(productMetrics);
    }

    @Override
    public void upsertLikeDeltas(Map<Long, Integer> likeDeltas) {
        if (likeDeltas.isEmpty()) {
            return;
        }

        String sql = """
            INSERT INTO product_metrics
                (product_id, like_count, order_count, view_count, total_order_quantity, created_at, updated_at)
            VALUES (?, GREATEST(?, 0), 0, 0, 0, NOW(), NOW())
            ON DUPLICATE KEY UPDATE
                like_count = like_count + VALUES(like_count),
                updated_at = NOW()
            """;

        List<Map.Entry<Long, Integer>> entries = new ArrayList<>(likeDeltas.entrySet());

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Map.Entry<Long, Integer> entry = entries.get(i);
                ps.setLong(1, entry.getKey());
                ps.setInt(2, entry.getValue());
            }

            @Override
            public int getBatchSize() {
                return entries.size();
            }
        });

        log.info("좋아요 수 Upsert 완료 - {} 건", entries.size());
    }

    @Override
    public void upsertViewDeltas(Map<Long, Integer> viewDeltas) {
        if (viewDeltas.isEmpty()) {
            return;
        }

        String sql = """
            INSERT INTO product_metrics
                (product_id, like_count, order_count, view_count, total_order_quantity, created_at, updated_at)
            VALUES (?, 0, 0, ?, 0, NOW(), NOW())
            ON DUPLICATE KEY UPDATE
                view_count = view_count + VALUES(view_count),
                updated_at = NOW()
            """;

        List<Map.Entry<Long, Integer>> entries = new ArrayList<>(viewDeltas.entrySet());

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Map.Entry<Long, Integer> entry = entries.get(i);
                ps.setLong(1, entry.getKey());
                ps.setInt(2, entry.getValue());
            }

            @Override
            public int getBatchSize() {
                return entries.size();
            }
        });

        log.info("조회 수 Upsert 완료 - {} 건", entries.size());
    }

    @Override
    public void upsertOrderDeltas(Map<Long, com.loopers.application.order.OrderMetrics> orderMetrics) {
        if (orderMetrics.isEmpty()) {
            return;
        }

        String sql = """
            INSERT INTO product_metrics
                (product_id, like_count, order_count, view_count, total_order_quantity, created_at, updated_at)
            VALUES (?, 0, ?, 0, ?, NOW(), NOW())
            ON DUPLICATE KEY UPDATE
                order_count = order_count + VALUES(order_count),
                total_order_quantity = total_order_quantity + VALUES(total_order_quantity),
                updated_at = NOW()
            """;

        List<Map.Entry<Long, com.loopers.application.order.OrderMetrics>> entries = new ArrayList<>(orderMetrics.entrySet());

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Map.Entry<Long, com.loopers.application.order.OrderMetrics> entry = entries.get(i);
                ps.setLong(1, entry.getKey());
                ps.setInt(2, entry.getValue().getOrderCount());      // 주문 건수
                ps.setInt(3, entry.getValue().getTotalQuantity());   // 총 주문 수량
            }

            @Override
            public int getBatchSize() {
                return entries.size();
            }
        });

        log.info("주문 메트릭 Upsert 완료 - {} 건 (건수와 수량 분리 처리)", entries.size());
    }
}
