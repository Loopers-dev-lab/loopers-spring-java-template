package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
public class Stock {

    @Column(nullable = false)
    private Integer quantity;

    @Builder
    private Stock(Integer quantity) {
        validate(quantity);
        this.quantity = quantity;
    }

    public static Stock createStock(Integer quantity) {
        return Stock.builder()
                .quantity(quantity)
                .build();
    }

    public void reduceQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "차감할 재고 수량은 1 이상이어야 합니다.");
        }

        if (this.quantity == null || this.quantity < quantity) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고가 부족합니다.");
        }

        this.quantity -= quantity;
    }

    private void validate(Integer quantity) {
        if (quantity == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고 수량은 필수입니다.");
        }
        if (quantity < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "재고 수량은 0 이상이어야 합니다.");
        }
    }
}
