package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Stock {

    @Column(name = "stock", nullable = false)
    private Integer value;

    private Stock(Integer value) {
        validate(value);
        this.value = value;
    }

    public static Stock of(Integer value) {
        return new Stock(value);
    }

    private void validate(Integer value) {
        if (value == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고 수량은 필수입니다.");
        }
        if (value < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고 수량은 음수일 수 없습니다.");
        }
    }

    public Stock decrease(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "차감 수량은 0보다 커야 합니다.");
        }
        if (this.value < quantity) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고가 부족합니다.");
        }
        return new Stock(this.value - quantity);
    }

    public boolean isAvailable(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "요청 수량은 0보다 커야 합니다.");
        }
        return this.value >= quantity;
    }
}
