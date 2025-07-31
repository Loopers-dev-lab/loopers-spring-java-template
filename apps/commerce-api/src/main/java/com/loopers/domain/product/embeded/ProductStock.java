package com.loopers.domain.product.embeded;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;

@Embeddable
public class ProductStock {
    
    @Column(name = "stock")
    private BigDecimal stock;
    
    private ProductStock(BigDecimal stock) {
        this.stock = stock;
    }
    
    public ProductStock() {
    }
    
    public static ProductStock of(BigDecimal stock) {
        validateStock(stock);
        return new ProductStock(stock);
    }

    public BigDecimal getValue() {
        return stock;
    }

    private static void validateStock(BigDecimal stock) {
        if (stock == null) {
            throw new CoreException(ErrorType.BAD_REQUEST,"재고는 필수입니다.");
        }
        if (stock.compareTo(BigDecimal.ZERO) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST,"재고는 0 이상이어야 합니다.");
        }
    }
    
    public boolean hasEnough(BigDecimal quantity) {
        if (quantity == null) {
            throw new CoreException(ErrorType.BAD_REQUEST,"수량은 필수입니다.");
        }
        if (quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST,"수량은 0보다 커야 합니다.");
        }
        return this.stock.compareTo(quantity) >= 0;
    }
    
    public ProductStock decrease(BigDecimal quantity) {
        if (!hasEnough(quantity)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고가 부족합니다.");
        }
        return new ProductStock(this.stock.subtract(quantity));
    }
    
    public ProductStock increase(BigDecimal quantity) {
        if (quantity == null || quantity.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST,"증가할 수량은 0보다 커야 합니다.");
        }
        return new ProductStock(this.stock.add(quantity));
    }
}
