package com.loopers.core.infra.database.mysql.product;

import com.loopers.core.infra.database.mysql.product.entity.ProductEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductBulkRepositoryImpl implements ProductBulkRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public void bulkSaveOrUpdate(List<ProductEntity> products) {
        if (products.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO product (brand_id, name, price, stock, like_count, created_at, updated_at, deleted_at) " +
                "VALUES (:brandId, :name, :price, :stock, :likeCount, :createdAt, :updatedAt, :deletedAt) " +
                "ON DUPLICATE KEY UPDATE " +
                "name = VALUES(name), " +
                "price = VALUES(price), " +
                "stock = VALUES(stock), " +
                "like_count = VALUES(like_count), " +
                "updated_at = VALUES(updated_at), " +
                "deleted_at = VALUES(deleted_at)";

        SqlParameterSource[] batchArgs = products.stream()
                .map(BeanPropertySqlParameterSource::new)
                .toArray(SqlParameterSource[]::new);

        jdbcTemplate.batchUpdate(sql, batchArgs);
    }
}
