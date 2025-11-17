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
public class Price {

    @Column(nullable = false)
    private Integer price;

    @Builder
    private Price(Integer price) {
        validate(price);
        this.price = price;
    }

    public static Price createPrice(Integer price) {
        return Price.builder()
                .price(price)
                .build();
    }

    private void validate(Integer price) {
        if (price == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "가격은 필수입니다.");
        }
        if (price <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "가격은 0보다 커야 합니다.");
        }
    }
}
