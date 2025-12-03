package com.loopers.domain.product;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Entity
@Table(name = "product")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {
    @Column(name = "ref_brand_id", nullable = false)
    private Long brandId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @Column(name = "stock", nullable = false)
    private int stock;

    @Column(name = "like_count", nullable = false)
    private int likeCount;


    public Product(Long brandId, String name, BigDecimal price, int stock, int likeCount) {
        this.brandId = brandId;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.likeCount = likeCount;
    }

    public Product changePrice(BigDecimal newPrice) {
        this.price = newPrice;
        return this;
    }

    public void decreaseStock(int amount) {
        if (amount <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고 감소량은 0보다 커야 합니다.");
        }
        if (this.stock < amount) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고가 부족합니다.");
        }
        this.stock -= amount;

    }

    public boolean isInStock(int quantity) {
        return this.stock >= quantity;
    }

    public int addLikeCount() {
        return this.likeCount++;
    }

}
