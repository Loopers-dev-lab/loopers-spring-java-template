package com.loopers.domain.order;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItemPrice {

    @Column(name = "item_price", nullable = false)
    private Long value;

    private OrderItemPrice(Long value) {
        validate(value);
        this.value = value;
    }

    public static OrderItemPrice of(Long value) {
        return new OrderItemPrice(value);
    }

    private void validate(Long value) {
        if (value == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "단가는 필수입니다.");
        }
        if (value < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "단가는 0 이상이어야 합니다.");
        }
    }
}
