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
public class PaymentStatus {
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    public PaymentStatus() {}
    
    private PaymentStatus(Status status) {
        this.status = status;
    }
    
    public static PaymentStatus of(String paymentStatus) {
        Status status = Status.from(paymentStatus);
        if (status == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "결제 상태를 확인해 주세요");
        }
        return new PaymentStatus(status);
    }
    
    public static PaymentStatus initiated() {
        return new PaymentStatus(Status.INITIATED);
    }
    
    public static PaymentStatus processing() {
        return new PaymentStatus(Status.PROCESSING);
    }
    
    public static PaymentStatus completed() {
        return new PaymentStatus(Status.COMPLETED);
    }
    
    public static PaymentStatus failed() {
        return new PaymentStatus(Status.FAILED);
    }
    
    public static PaymentStatus cancelled() {
        return new PaymentStatus(Status.CANCELLED);
    }
    
    public boolean isCompleted() {
        return this.status == Status.COMPLETED;
    }
    
    public boolean isPending() {
        return this.status == Status.INITIATED || this.status == Status.PROCESSING;
    }
    
    public boolean isFailed() {
        return this.status == Status.FAILED;
    }
    
    public boolean isCancelled() {
        return this.status == Status.CANCELLED;
    }
    
    public PaymentStatus complete() {
        return new PaymentStatus(Status.COMPLETED);
    }
    
    public PaymentStatus fail() {
        return new PaymentStatus(Status.FAILED);
    }
    
    public PaymentStatus cancel() {
        return new PaymentStatus(Status.CANCELLED);
    }
    
    public String getValue() {
        return this.status.name();
    }
    
    public enum Status {
        INITIATED,      // "결제 시작"
        PROCESSING,     // "결제 처리중"
        COMPLETED,      // "결제 완료"
        FAILED,         // "결제 실패"
        CANCELLED;      // "결제 취소"
        
        public static Status from(String status) {
            if (status == null) return null;
            try {
                return valueOf(status.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
    }
}
