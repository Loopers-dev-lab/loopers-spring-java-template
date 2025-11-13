package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@EqualsAndHashCode
public class Stock {

    @Column(name = "stock", nullable = false, columnDefinition = "int default 0")
    private int quantity;

    private Stock(int quantity) {
        validateQuantity(quantity);
        this.quantity = quantity;
    }

    public static Stock of(int quantity) {
        return new Stock(quantity);
    }

    public Stock increase(int amount) {
        if (amount <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고 증가량은 양수여야 합니다");
        }
        return new Stock(this.quantity + amount);
    }

    public Stock decrease(int amount) {
        if (amount <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고 감소량은 양수여야 합니다");
        }

        if (!isSufficient(amount)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고가 부족합니다. 현재 재고: " + this.quantity);
        }

        return new Stock(this.quantity - amount);
    }

    public boolean isSufficient(int required) {
        return this.quantity >= required;
    }

    private void validateQuantity(int quantity) {
        if (quantity < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 재고는 음수일 수 없습니다");
        }
    }
}
