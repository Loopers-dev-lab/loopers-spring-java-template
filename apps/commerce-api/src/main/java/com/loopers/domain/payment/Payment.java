package com.loopers.domain.payment;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.order.Order;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "payment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Payment extends BaseEntity {

    // private String pgTransactionId;
    
    @Enumerated(EnumType.STRING)
    private PaymentMethod method;
    
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Builder
    private Payment(
            // String pgTransactionId,
            PaymentMethod method,
            PaymentStatus paymentStatus,
            Order order
    ) {
        // this.pgTransactionId = pgTransactionId;
        this.method = method;
        this.paymentStatus = paymentStatus;
        this.order = order;
        if(order != null) {
            this.amount = order.getFinalAmount();
        }
        guard();
    }

    @Override
    protected void guard() {
        // method 검증: null이 아니어야 함
        if (method == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Payment : method가 비어있을 수 없습니다.");
        }

        // amount 검증: null이 아니어야 하며, 0 이상이어야 함
        if (amount == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Payment : amount가 비어있을 수 없습니다.");
        } else if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Payment : amount는 음수가 될 수 없습니다.");
        }

        // paymentStatus 검증: null이 아니어야 함
        if (paymentStatus == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Payment : paymentStatus가 비어있을 수 없습니다.");
        }

        // order 검증: null이 아니어야 함 (결제 대상 주문 정보 필수)
        if (order == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Payment : order가 비어있을 수 없습니다.");
        }
    }
}

