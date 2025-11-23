package com.loopers.domain.product;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.common.vo.Money;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "products")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "brand_id", nullable = false)
    private Long brandId;
    @Column(nullable = false)
    private String name;
    @Embedded
    private Money price;

    private int likeCount;

    @Version
    private Long version;

    public Product(Long brandId, String name, Money price) {
        if (brandId == null || brandId <= 0) {
            throw new IllegalArgumentException("브랜드 ID는 음수 혹은 null일 수 없습니다.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("상품명은 비어있을 수 없습니다.");
        }
        if (price == null) {
            throw new IllegalArgumentException("상품가격은 비어있을 수 없습니다.");
        }
        this.brandId = brandId;
        this.name = name;
        this.price = price;
        this.likeCount = 0;
    }

    public static Product create(Long brandId, String name, Money price) {
        return new Product(brandId, name, price);
    }

    public void increaseLikeCount() {
        this.likeCount += 1;
    }

    public void decreaseLikeCount() {
        if (this.likeCount <= 0) {
            throw new IllegalStateException("좋아요 수는 0 미만일 수 없습니다.");
        }
        this.likeCount -= 1;
    }
}
