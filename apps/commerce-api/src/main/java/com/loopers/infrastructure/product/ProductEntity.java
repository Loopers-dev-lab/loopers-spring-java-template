package com.loopers.infrastructure.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 상품 JPA Entity
 */
@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_brand_id", columnList = "brand_id"),
        @Index(name = "idx_brand_id_like_count", columnList = "brand_id, like_count DESC"),
        @Index(name = "idx_status", columnList = "status")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stock;

    @Column(length = 500)
    private String imageUrl;

    @Column(name = "brand_id", nullable = false)
    private Long brandId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status;

    // 비정규화 필드: 좋아요 수
    @Column(name = "like_count", nullable = false)
    private Integer likeCount = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime modifiedAt;

    // === 생성자 ===

    private ProductEntity(
            String name,
            String description,
            BigDecimal price,
            Integer stock,
            String imageUrl,
            Long brandId,
            ProductStatus status,
            Integer likeCount,
            LocalDateTime createdAt,
            LocalDateTime modifiedAt
    ) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
        this.imageUrl = imageUrl;
        this.brandId = brandId;
        this.status = status;
        this.likeCount = likeCount;
        this.createdAt = createdAt;
        this.modifiedAt = modifiedAt;
    }

    // === Domain <-> Entity 변환 ===

    /**
     * Domain 객체로부터 Entity 생성
     */
    public static ProductEntity from(Product product) {
        return new ProductEntity(
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getImageUrl(),
                product.getBrandId(),
                product.getStatus(),
                product.getLikeCount(),
                product.getCreatedAt(),
                product.getModifiedAt()
        );
    }

    /**
     * Entity를 Domain 객체로 변환
     */
    public Product toDomain() {
        return Product.reconstitute(
                this.id,
                this.name,
                this.description,
                this.price,
                this.stock,
                this.imageUrl,
                this.brandId,
                this.status,
                this.likeCount,
                this.createdAt,
                this.modifiedAt
        );
    }

    /**
     * Domain 객체로부터 Entity 업데이트
     */
    public void updateFrom(Product product) {
        this.stock = product.getStock();
        this.likeCount = product.getLikeCount();
        this.modifiedAt = product.getModifiedAt();
    }
}
