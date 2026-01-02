package com.loopers.batch.job.ranking;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamWriter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

public class RankingMaterializedViewWriter implements ItemStreamWriter<ProductRankAggregate> {

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final String PERIOD_COLUMN = "period_key";

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final RankingPeriod period;
    private final String tableName;
    private final AtomicInteger rankCounter = new AtomicInteger(1);

    public RankingMaterializedViewWriter(
            NamedParameterJdbcTemplate jdbcTemplate,
            RankingPeriod period,
            String tableName
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.period = period;
        this.tableName = tableName;
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        deleteExistingRows();
        rankCounter.set(1);
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        // no-op
    }

    @Override
    public void close() throws ItemStreamException {
        // no-op
    }

    @Override
    public void write(Chunk<? extends ProductRankAggregate> chunk) {
        if (chunk == null || chunk.isEmpty()) {
            return;
        }
        LocalDateTime aggregatedAt = LocalDateTime.now(ZONE_ID);
        List<SqlParameterSource> params = new ArrayList<>();
        for (ProductRankAggregate item : chunk.getItems()) {
            if (item == null || item.productId() == null) {
                continue;
            }
            MapSqlParameterSource paramSource = new MapSqlParameterSource()
                    .addValue("periodKey", period.key())
                    .addValue("productId", item.productId())
                    .addValue("likeCount", item.likeCount())
                    .addValue("salesCount", item.salesCount())
                    .addValue("score", item.score())
                    .addValue("rank", rankCounter.getAndIncrement())
                    .addValue("aggregatedAt", aggregatedAt);
            params.add(paramSource);
        }
        if (!params.isEmpty()) {
            jdbcTemplate.batchUpdate(insertSql(), params.toArray(SqlParameterSource[]::new));
        }
    }

    private void deleteExistingRows() {
        jdbcTemplate.update(
                "DELETE FROM " + tableName + " WHERE " + PERIOD_COLUMN + " = :periodKey",
                new MapSqlParameterSource("periodKey", period.key())
        );
    }

    private String insertSql() {
        return """
                INSERT INTO %s (%s, product_id, like_count, sales_count, score, rank, aggregated_at)
                VALUES (:periodKey, :productId, :likeCount, :salesCount, :score, :rank, :aggregatedAt)
                """.formatted(tableName, PERIOD_COLUMN);
    }
}
