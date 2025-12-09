package com.loopers.core.infra.database.mysql.productlike;

import com.loopers.core.domain.productlike.ProductLikeCache;
import com.loopers.core.infra.database.mysql.productlike.entity.ProductLikeEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductLikeBulkRepositoryImpl implements ProductLikeBulkRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public void bulkSaveOrUpdate(List<ProductLikeEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO product_like (product_id, user_id, created_at)
                VALUES (:productId, :userId, :createdAt)
                ON DUPLICATE KEY UPDATE
                created_at = VALUES(created_at)
                """;

        SqlParameterSource[] batchArgs = entities.stream()
                .map(entity -> new MapSqlParameterSource()
                        .addValue("productId", entity.getProductId())
                        .addValue("userId", entity.getUserId())
                        .addValue("createdAt", entity.getCreatedAt())
                )
                .toArray(SqlParameterSource[]::new);

        jdbcTemplate.batchUpdate(sql, batchArgs);
    }

    @Override
    public void bulkDelete(List<ProductLikeCache> caches) {
        if (caches == null || caches.isEmpty()) {
            return;
        }

        String sql = """
                DELETE FROM product_like
                WHERE product_id = :productId AND user_id = :userId
                """;

        SqlParameterSource[] batchArgs = caches.stream()
                .map(cache -> new MapSqlParameterSource()
                        .addValue("productId", cache.productId().value())
                        .addValue("userId", cache.userId().value())
                )
                .toArray(SqlParameterSource[]::new);

        jdbcTemplate.batchUpdate(sql, batchArgs);
    }
}
