package com.loopers.core.infra.database.mysql.product;

import com.loopers.core.infra.database.mysql.product.entity.WeeklyProductMetricEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class WeeklyProductMetricBulkRepositoryImpl implements WeeklyProductMetricBulkRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public void bulkUpsert(List<WeeklyProductMetricEntity> weeklyProductMetrics) {
        if (Objects.isNull(weeklyProductMetrics) || weeklyProductMetrics.isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO weekly_product_metrics
                (product_id, year, month, week_of_year, like_count, total_sales_count, view_count, created_at, updated_at)
                VALUES
                (:productId, :year, :month, :weekOfYear, :likeCount, :totalSalesCount, :viewCount, :createdAt, :updatedAt)
                ON DUPLICATE KEY UPDATE
                like_count = VALUES(like_count),
                total_sales_count = VALUES(total_sales_count),
                view_count = VALUES(view_count),
                updated_at = VALUES(updated_at)
                """;

        SqlParameterSource[] batch = weeklyProductMetrics.stream()
                .map(metric -> new MapSqlParameterSource()
                        .addValue("productId", metric.getProductId())
                        .addValue("year", metric.getYear())
                        .addValue("month", metric.getMonth())
                        .addValue("weekOfYear", metric.getWeekOfYear())
                        .addValue("likeCount", metric.getLikeCount())
                        .addValue("totalSalesCount", metric.getTotalSalesCount())
                        .addValue("viewCount", metric.getViewCount())
                        .addValue("createdAt", metric.getCreatedAt())
                        .addValue("updatedAt", LocalDateTime.now())
                )
                .toArray(SqlParameterSource[]::new);

        jdbcTemplate.batchUpdate(sql, batch);
    }
}
