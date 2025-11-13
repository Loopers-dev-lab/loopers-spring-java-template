package com.loopers.domain.product;

import jakarta.persistence.Embeddable;

@Embeddable
public record Stock(int quantity) {

    public Stock {
        if (quantity < 0) {
            throw new IllegalArgumentException("재고 수량은 음수일 수 없습니다.");
        }
    }

    public Stock decrease(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("감소량은 1 이상이어야 합니다.");
        }
    }
}

