package com.loopers.domain.payment.embedded;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

import java.math.BigDecimal;

@Embeddable
@Getter
public class PaymentPointUsed {
    
    @Column(name = "points_used", nullable = false)
    private BigDecimal pointsUsed;

    public PaymentPointUsed() {}
    
    private PaymentPointUsed(BigDecimal pointsUsed) {
        this.pointsUsed = pointsUsed;
    }
    
    public static PaymentPointUsed of(BigDecimal pointsUsed) {
        validatePointsUsed(pointsUsed);
        return new PaymentPointUsed(pointsUsed);
    }
    
    public static PaymentPointUsed of(int pointsUsed) {
        return of(BigDecimal.valueOf(pointsUsed));
    }
    
    public static PaymentPointUsed zero() {
        return new PaymentPointUsed(BigDecimal.ZERO);
    }
    
    public boolean isZero() {
        return this.pointsUsed.compareTo(BigDecimal.ZERO) == 0;
    }
    
    public boolean isPositive() {
        return this.pointsUsed.compareTo(BigDecimal.ZERO) > 0;
    }
    
    public boolean isGreaterThan(PaymentPointUsed other) {
        if (other == null) return true;
        return this.pointsUsed.compareTo(other.pointsUsed) > 0;
    }
    
    public boolean isLessThanOrEqual(PaymentAmount totalAmount) {
        if (totalAmount == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "총 결제 금액이 필요합니다.");
        }
        return this.pointsUsed.compareTo(totalAmount.getValue()) <= 0;
    }
    
    public BigDecimal getValue() {
        return pointsUsed;
    }
    
    public int getIntValue() {
        return pointsUsed.intValue();
    }
    
    private static void validatePointsUsed(BigDecimal pointsUsed) {
        if (pointsUsed == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용된 포인트는 필수입니다.");
        }
        if (pointsUsed.compareTo(BigDecimal.ZERO) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용된 포인트는 0 이상이어야 합니다.");
        }
    }
}
