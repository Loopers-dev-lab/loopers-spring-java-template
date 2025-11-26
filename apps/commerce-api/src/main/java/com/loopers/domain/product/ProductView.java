package com.loopers.domain.product;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Entity
@Table(name = "product_view", indexes = {
    @Index(name = "idx_ps_like_desc", columnList = "likeCount DESC"),
    @Index(name = "idx_ps_brand_like", columnList = "brandId, likeCount DESC")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ProductView {

    @Id
    private Long id; // Original Product ID

    private String name;
    private BigDecimal price;
    private Long likeCount;

    private Long brandId;
    private String brandName;

    @Enumerated(EnumType.STRING)
    private ProductStatus status;

    @Column(name = "created_at")
    private ZonedDateTime createdAt;

    // 업데이트 편의 메서드
    public void updateLikeCount(Long newCount) {
        this.likeCount = newCount;
    }

    public void updateBrandName(String brandName) {
        this.brandName = brandName;
    }
}

