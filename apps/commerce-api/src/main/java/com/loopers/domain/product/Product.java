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

    private Brand brandId;

    @Column(name = "name", nullable = false)
    private String name;

    @Embedded
    private LikeCount likeCount;

    @Embedded
    private Stock stock;

    @Builder
    public Product(Brand brandId, String name, LikeCount likeCount, Stock stock) {
        this.brandId = brandId;
        this.name = name;
        this.likeCount = likeCount;
        this.stock = stock;
    }

    public void decreaseStock(int quantity) {
        this.stock = this.stock.decrease(quantity);
    }
}
