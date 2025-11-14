package com.loopers.domain.product;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.brand.Brand;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorMessages;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@Getter
@Table(name = "product")
public class Product extends BaseEntity {
    
    @ManyToOne
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @Column(name = "name", nullable = false)
    private String name;

    @Embedded
    private Stock stock;

    @Embedded
    private Money price;

    @Builder
    public Product(Brand brand, String name, Stock stock, Money price) {
        validateProductName(name);

        this.brand = brand;
        this.name = name;
        this.stock = stock;
        this.price = price;
    }

    private void validateProductName(String name) {
        if (name == null || name.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, ErrorMessages.INVALID_NAME_FORMAT);
        }
    }


    public void decreaseStock(int quantity) {
        this.stock.decrease(quantity);
    }
}
