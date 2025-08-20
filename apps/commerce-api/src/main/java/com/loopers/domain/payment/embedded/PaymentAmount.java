package com.loopers.domain.payment.embedded;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

import java.math.BigDecimal;

@Embeddable
@Getter
public class PaymentAmount {
    
    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    public PaymentAmount() {}
    
    private PaymentAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public static PaymentAmount of(BigDecimal amount) {
        validateAmount(amount);
        return new PaymentAmount(amount);
    }
    
    public static PaymentAmount of(int amount) {
        return of(BigDecimal.valueOf(amount));
    }
    
    public static PaymentAmount zero() {
        return new PaymentAmount(BigDecimal.ZERO);
    }
    
    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }
    
    public boolean isPositive() {
        return this.amount.compareTo(BigDecimal.ZERO) > 0;
    }
    
    public PaymentAmount add(PaymentAmount other) {
        if (other == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "더할 금액이 필요합니다.");
        }
        return new PaymentAmount(this.amount.add(other.amount));
    }
    
    public PaymentAmount subtract(PaymentAmount other) {
        if (other == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "뺄 금액이 필요합니다.");
        }
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "결제 금액은 0 이상이어야 합니다.");
        }
        return new PaymentAmount(result);
    }
    
    public BigDecimal getValue() {
        return amount;
    }
    
    public int getIntValue() {
        return amount.intValue();
    }
    
    private static void validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "결제 금액은 필수입니다.");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "결제 금액은 0 이상이어야 합니다.");
        }
    }
}
