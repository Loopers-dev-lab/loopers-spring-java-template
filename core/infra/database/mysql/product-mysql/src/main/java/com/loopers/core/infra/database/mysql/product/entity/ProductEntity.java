package com.loopers.core.infra.database.mysql.product.entity;

import com.loopers.core.domain.brand.vo.BrandId;
import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.DeletedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.product.Product;
import com.loopers.core.domain.product.vo.*;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Entity
@Table(
        name = "product",
        indexes = {
                @Index(name = "idx_product_brand_id", columnList = "brand_id"),
                @Index(name = "idx_product_name", columnList = "name"),
                @Index(name = "idx_product_price", columnList = "price"),
                @Index(name = "idx_product_like_count", columnList = "like_count"),
                @Index(name = "idx_product_created_at", columnList = "created_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long brandId;

    @Column(nullable = false)
    private String name;

    @Column(precision = 19, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Long stock;

    @Column(nullable = false)
    private Long likeCount;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    public static ProductEntity from(Product product) {
        return new ProductEntity(
                Optional.ofNullable(product.getProductId().value())
                        .map(Long::parseLong)
                        .orElse(null),
                Long.parseLong(product.getBrandId().value()),
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
                new ProductId(this.id.toString()),
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
