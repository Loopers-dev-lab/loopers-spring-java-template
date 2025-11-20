package com.loopers.domain.product.vo;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock {
    private long quantity;

    public Stock(long quantity) {
        if (quantity < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고는 0보다 작을 수 없습니다.");
        }
        this.quantity = quantity;
    }

    public Stock decrease(long amount) {
        if (amount <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "차감하려는 수량은 0보다 커야합니다.");
        }
        if (this.quantity < amount) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고가 부족합니다.");
        }
        return new Stock(this.quantity - amount);
    }
}
