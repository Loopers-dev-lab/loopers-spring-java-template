package com.loopers.domain.product.embeded;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;

@Embeddable
public class ProductPrice {
    private BigDecimal price;

    private ProductPrice(BigDecimal price) {
        this.price = price;
    }

    public ProductPrice() {
    }

    public static ProductPrice of(BigDecimal price) {
        if(price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "price는 0보다 커야합니다.");
        }
        return new ProductPrice(price);
    }
    public BigDecimal getValue() {
        return this.price;
    }
}
