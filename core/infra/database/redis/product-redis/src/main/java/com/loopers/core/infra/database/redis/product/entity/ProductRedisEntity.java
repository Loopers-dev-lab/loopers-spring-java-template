package com.loopers.core.infra.database.redis.product.entity;

import com.loopers.core.domain.brand.vo.BrandId;
import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.DeletedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.product.Product;
import com.loopers.core.domain.product.vo.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ProductRedisEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private Long brandId;
    private String name;
    private BigDecimal price;
    private Long stock;
    private Long likeCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public static ProductRedisEntity from(Product product) {
        return new ProductRedisEntity(
                Optional.ofNullable(product.getId().value())
                        .map(String::valueOf)
                        .orElse(null),
                Long.parseLong(Objects.requireNonNull(product.getBrandId().value())),
                product.getName().value(),
                product.getPrice().value(),
                product.getStock().value(),
                product.getLikeCount().value(),
                product.getCreatedAt().value(),
                product.getUpdatedAt().value(),
                product.getDeletedAt().value()
        );
    }

    public Product to() {
        return Product.mappedBy(
                new ProductId(this.id),
                new BrandId(this.brandId.toString()),
                new ProductName(this.name),
                new ProductPrice(this.price),
                new ProductStock(this.stock),
                new ProductLikeCount(this.likeCount),
                new CreatedAt(this.createdAt),
                new UpdatedAt(this.updatedAt),
                new DeletedAt(this.deletedAt)
        );
    }
}
