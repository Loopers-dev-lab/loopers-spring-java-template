package com.loopers.domain.order.embeded;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;

@Embeddable
@Getter
public class OrderStatus {
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;
    
    protected OrderStatus() {}
    
    private OrderStatus(Status status) {
        this.status = status;
    }
    
    public static OrderStatus of(String orderStatus) {
        Status status = Status.from(orderStatus);
        if (status == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 상태를 확인해 주세요");
        }

        return new OrderStatus(status);
    }
    
    public static OrderStatus pendingPayment() {
        return new OrderStatus(Status.PENDING_PAYMENT);
    }

    public boolean isPendingPayment() {
        return this.status == Status.PENDING_PAYMENT;
    }
    
    public boolean isPaymentCompleted() {
        return this.status == Status.PAYMENT_COMPLETED;
    }
    
    public boolean isCancelled() {
        return this.status == Status.CANCELLED;
    }

    public boolean canBeCancelled() {
        return this.status == Status.PENDING_PAYMENT;
    }
    
    public String getValue() {
        return this.status.name();
    }
    public OrderStatus cancel(){
        return new OrderStatus(Status.CANCELLED);
    }
    public OrderStatus updateStatus(String orderStatus) {
        Status status = Status.from(orderStatus);
        if (status == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 상태는 필수입니다.");
        }
        return new OrderStatus(status);
    }
    
    public enum Status {
        PENDING_PAYMENT,//"결제 대기"
        PAYMENT_COMPLETED,//"결제 완료"
        PAYMENT_FAILED,//"결제 실패"
        CANCELLED,//"취소됨"
        EXPIRED,//"만료됨"
        ;
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
