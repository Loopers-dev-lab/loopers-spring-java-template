package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorMessages;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
public class Stock {

    @Column(name = "stock_quantity", nullable = false)
    private int quantity;

    public Stock(int quantity) {
        if (quantity < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, ErrorMessages.INVALID_STOCK_QUANTITY);
        }
        this.quantity = quantity;
    }

    public void decrease(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("감소량은 1 이상이어야 합니다.");
        }
        int newQuantity = this.quantity - quantity;
        if (newQuantity < 0) {
            throw new IllegalArgumentException("재고 수량은 음수일 수 없습니다.");
        }
        this.quantity = newQuantity;
    }
}

