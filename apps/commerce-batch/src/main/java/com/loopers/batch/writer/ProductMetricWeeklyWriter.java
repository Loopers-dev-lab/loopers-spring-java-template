package com.loopers.batch.writer;

import com.loopers.batch.dto.ProductRankRow;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;

@Component
@StepScope
@RequiredArgsConstructor
public class ProductMetricWeeklyWriter implements ItemWriter<ProductRankRow> {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Value("#{jobParameters['weeklyStartDate']}")
    private String startDate;

    @Value("#{jobParameters['weeklyEndDate']}")
    private String endDate;

    private static final String SQL = """
        INSERT INTO mv_product_rank_weekly
          (start_date, end_date, ref_product_id,
           view_count_sum, like_count_sum, sales_volume_sum,
           score, created_at, updated_at, deleted_at)
        VALUES
          (:startDate, :endDate, :productId,
           :viewCountSum, :likeCountSum, :salesVolumeSum,
           :score, :createdAt, :updatedAt, NULL)
        """;

    @Override
    public void write(Chunk<? extends ProductRankRow> chunk) {
        MapSqlParameterSource[] batch = chunk.getItems().stream()
                .map(item -> new MapSqlParameterSource()
                        .addValue("startDate", startDate)
                        .addValue("endDate", endDate)
                        .addValue("productId", item.productId())
                        .addValue("viewCountSum", item.viewCountSum())
                        .addValue("likeCountSum", item.likeCountSum())
                        .addValue("salesVolumeSum", item.salesVolumeSum())
                        .addValue("score", item.score())
                        .addValue("createdAt", ZonedDateTime.now())
                        .addValue("updatedAt", ZonedDateTime.now()))
                .toArray(MapSqlParameterSource[]::new);

        jdbcTemplate.batchUpdate(SQL, batch);
    }

}
