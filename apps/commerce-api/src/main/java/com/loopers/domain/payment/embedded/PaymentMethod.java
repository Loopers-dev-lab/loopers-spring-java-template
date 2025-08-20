package com.loopers.domain.payment.embedded;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;

@Embeddable
@Getter
public class PaymentMethod {
    
    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false)
    private Method method;

    public PaymentMethod() {}
    
    private PaymentMethod(Method method) {
        this.method = method;
    }
    
    public static PaymentMethod of(String paymentMethod) {
        Method method = Method.from(paymentMethod);
        if (method == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "결제 수단을 확인해 주세요");
        }
        return new PaymentMethod(method);
    }
    
    public static PaymentMethod card() {
        return new PaymentMethod(Method.CARD);
    }
    
    public static PaymentMethod point() {
        return new PaymentMethod(Method.POINT);
    }
    
    public static PaymentMethod mixed() {
        return new PaymentMethod(Method.MIXED);
    }
    
    public boolean isCard() {
        return this.method == Method.CARD;
    }
    
    public boolean isPoint() {
        return this.method == Method.POINT;
    }
    
    public boolean isMixed() {
        return this.method == Method.MIXED;
    }
    
    public boolean requiresPointUsage() {
        return this.method == Method.POINT || this.method == Method.MIXED;
    }
    
    public boolean requiresCardPayment() {
        return this.method == Method.CARD || this.method == Method.MIXED;
    }
    
    public String getValue() {
        return this.method.name();
    }
    
    public enum Method {
        CARD,    // "카드 결제"
        POINT,   // "포인트 결제"
        MIXED;   // "복합 결제 (카드 + 포인트)"
        
        public static Method from(String method) {
            if (method == null) return null;
            try {
                return valueOf(method.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }
}
