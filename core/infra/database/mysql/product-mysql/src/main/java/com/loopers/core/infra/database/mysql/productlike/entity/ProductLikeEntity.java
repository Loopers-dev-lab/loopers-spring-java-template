package com.loopers.core.infra.database.mysql.productlike.entity;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.product.vo.ProductId;
import com.loopers.core.domain.productlike.ProductLike;
import com.loopers.core.domain.productlike.vo.ProductLikeId;
import com.loopers.core.domain.user.vo.UserId;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Entity
@Table(
        name = "product_like",
        indexes = {
                @Index(name = "idx_product_like_product_user", columnList = "product_id, user_id"),
                @Index(name = "idx_product_like_product_id", columnList = "product_id"),
                @Index(name = "idx_product_like_user_id", columnList = "user_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ProductLikeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public static ProductLikeEntity from(ProductLike productLike) {
        return new ProductLikeEntity(
                Optional.ofNullable(productLike.getId().value())
                        .map(Long::parseLong)
                        .orElse(null),
                Long.parseLong(Objects.requireNonNull(productLike.getProductId().value())),
                Long.parseLong(Objects.requireNonNull(productLike.getUserId().value())),
                productLike.getCreatedAt().value()
        );
    }

    public ProductLike to() {
        return ProductLike.mappedBy(
                new ProductLikeId(this.id.toString()),
                new UserId(this.userId.toString()),
                new ProductId(this.productId.toString()),
                new CreatedAt(this.createdAt)
        );
    }
}
