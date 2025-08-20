package com.loopers.domain.payment.embedded;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;

@Embeddable
@Getter
public class PaymentOrderId {
    
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    public PaymentOrderId() {}
    
    private PaymentOrderId(Long orderId) {
        this.orderId = orderId;
    }
    
    public static PaymentOrderId of(Long orderId) {
        validateOrderId(orderId);
        return new PaymentOrderId(orderId);
    }
    
    public Long getValue() {
        return orderId;
    }
    
    private static void validateOrderId(Long orderId) {
        if (orderId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 ID는 필수입니다.");
        }
        if (orderId <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 ID는 양수여야 합니다.");
        }
    }
}
