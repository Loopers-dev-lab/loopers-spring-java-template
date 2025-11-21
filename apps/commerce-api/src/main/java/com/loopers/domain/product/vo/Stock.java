package com.loopers.domain.product.vo;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Embeddable
@NoArgsConstructor
@Getter
public class Stock {

    private int quantity;

    private Stock(int quantity) {
        if (quantity < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고는 0 이상이어야 합니다.");
        }
        this.quantity = quantity;
    }

    public static Stock of(int quantity) {
        return new Stock(quantity);
    }

    public Stock decrease(int decreaseAmount) {
        if (decreaseAmount <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "차감할 재고는 0보다 커야 합니다.");
        }
        int newQuantity = this.quantity - decreaseAmount;
        if (newQuantity < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고가 부족합니다.");
        }
        return new Stock(newQuantity);
    }

    public Stock increase(int increaseAmount) {
        if (increaseAmount <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "증가할 재고는 0보다 커야 합니다.");
        }
        return new Stock(this.quantity + increaseAmount);
    }

    public boolean isAvailable(int requiredQuantity) {
        return this.quantity >= requiredQuantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Stock stock = (Stock) o;
        return quantity == stock.quantity;
    }

    @Override
    public int hashCode() {
        return Objects.hash(quantity);
    }

    @Override
    public String toString() {
        return "Stock{quantity=" + quantity + "}";
    }
}
