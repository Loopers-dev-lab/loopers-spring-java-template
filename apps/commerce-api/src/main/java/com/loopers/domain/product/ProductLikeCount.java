package com.loopers.domain.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "product_like_counts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductLikeCount {

    @Id
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "brand_id")
    private Long brandId;

    @Column(name = "like_count", nullable = false)
    private int likeCount;

    public ProductLikeCount(Long productId, Long brandId) {
        this.productId = productId;
        this.brandId = brandId;
        this.likeCount = 0;
    }

    public static ProductLikeCount create(Long productId, Long brandId) {
        return new ProductLikeCount(productId, brandId);
    }
}
