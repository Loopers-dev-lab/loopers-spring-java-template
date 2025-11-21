package com.loopers.domain.product;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@Getter
@Table(name = "product")
public class Product extends BaseEntity {

    @Column(name = "brand_id", nullable = false)
    private Long brandId;

    @Column(name = "name", nullable = false)
    private String name;

    @Embedded
    private Money price;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "quantity", column = @Column(name = "stock_quantity", nullable = false))
    })
    private Stock stockQuantity;

    @Column(name = "like_count", nullable = false)
    private Long likeCount = 0L;

    @Builder
    public Product(Long brandId, String name, Money price, Stock stockQuantity, Long likeCount) {
        this.brandId = brandId;
        this.name = name;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.likeCount = likeCount != null ? likeCount : 0L;
    }

    public void decreaseStock(int quantity) {
        this.stockQuantity.decrease(quantity);
    }

    public int getStockQuantity() {
        return stockQuantity.getQuantity();
    }

    public Money getPrice() {
        return price;
    }

    public Long getLikeCount() {
        return likeCount;
    }

    public void increaseLikeCount() {
        this.likeCount = (this.likeCount == null ? 1L : this.likeCount + 1L);
    }

    public void decreaseLikeCount() {
        long current = this.likeCount == null ? 0L : this.likeCount;
        this.likeCount = Math.max(0L, current - 1L);
    }
}
