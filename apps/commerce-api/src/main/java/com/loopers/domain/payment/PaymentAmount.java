package com.loopers.domain.payment;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class PaymentAmount {

    @Column(name = "payment_amount", nullable = false)
    private Long value;

    private PaymentAmount(Long value) {
        validate(value);
        this.value = value;
    }

    public static PaymentAmount of(Long value) {
        return new PaymentAmount(value);
    }

    private void validate(Long value) {
        if (value == null || value <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "결제 금액은 0보다 커야 합니다.");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PaymentAmount that = (PaymentAmount) o;
        return value.equals(that.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
