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
    @AttributeOverrides({
        @AttributeOverride(name = "quantity", column = @Column(name = "stock_quantity", nullable = false))
    })
    private Stock stockQuantity;

    @Builder
    public Product(Long brandId, String name, Stock stockQuantity, Long totalLikes) {
        this.brandId = brandId;
        this.name = name;
        this.stockQuantity = stockQuantity;
    }

    public void decreaseStock(int quantity) {
        this.stockQuantity.decrease(quantity);
    }
}
