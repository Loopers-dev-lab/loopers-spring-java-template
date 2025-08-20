package com.loopers.domain.payment.embedded;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

@Embeddable
@Getter
public class PaymentSuccess {
    
    @Column(name = "success", nullable = false)
    private Boolean success;

    public PaymentSuccess() {}
    
    private PaymentSuccess(Boolean success) {
        this.success = success;
    }
    
    public static PaymentSuccess of(Boolean success) {
        if (success == null) {
            return failure(); // null인 경우 실패로 처리
        }
        return new PaymentSuccess(success);
    }
    
    public static PaymentSuccess success() {
        return new PaymentSuccess(true);
    }
    
    public static PaymentSuccess failure() {
        return new PaymentSuccess(false);
    }
    
    public boolean isSuccess() {
        return Boolean.TRUE.equals(this.success);
    }
    
    public boolean isFailure() {
        return !isSuccess();
    }
    
    public Boolean getValue() {
        return success;
    }
}
