package com.loopers.domain.payment;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.order.Order;
import com.loopers.domain.point.Point;
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

    private String pgTransactionId;
    
    @Enumerated(EnumType.STRING)
    private PaymentMethod method;
    
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Builder
    private Payment(
            String pgTransactionId,
            PaymentMethod method,
            BigDecimal amount,
            PaymentStatus paymentStatus,
            Order order
    ) {
        this.pgTransactionId = pgTransactionId;
        this.method = method;
        this.amount = amount;
        this.paymentStatus = paymentStatus;
        this.order = order;
        guard();
    }

    public void setOrder(Order order) {
        if (this.order != null) {
            throw new CoreException(ErrorType.CONFLICT, "Payment : Order가 이미 존재합니다.");
        }
        this.order = order;
    }

    /**
     * 결제 금액 계산
     * totalPrice - discountAmount + shippingFee = finalAmount
     */
    public static BigDecimal calculateFinalAmount(
            BigDecimal totalPrice,
            BigDecimal discountAmount,
            BigDecimal shippingFee
    ) {
        if (totalPrice == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Payment : totalPrice가 비어있을 수 없습니다.");
        }
        if (discountAmount == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Payment : discountAmount가 비어있을 수 없습니다.");
        }
        if (shippingFee == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Payment : shippingFee가 비어있을 수 없습니다.");
        }

        BigDecimal finalAmount = totalPrice.subtract(discountAmount).add(shippingFee);
        
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Payment : 최종 결제 금액은 음수가 될 수 없습니다.");
        }
        
        return finalAmount;
    }

    /**
     * Payment 생성 및 계산
     */
    public static Payment create(
            Order order,
            BigDecimal totalPrice,
            BigDecimal discountAmount,
            BigDecimal shippingFee,
            PaymentMethod method
    ) {
        BigDecimal finalAmount = calculateFinalAmount(totalPrice, discountAmount, shippingFee);
        
        return Payment.builder()
                .method(method)
                .amount(finalAmount)
                .paymentStatus(PaymentStatus.PENDING)
                .order(order)
                .build();
    }

    /**
     * 포인트 결제 처리
     * 포인트 차감 및 결제 상태 변경
     */
    public void processWithPoint(Point point) {
        if (this.paymentStatus != PaymentStatus.PENDING) {
            throw new CoreException(
                    ErrorType.BAD_REQUEST,
                    "Payment : PENDING 상태의 결제만 처리할 수 있습니다."
            );
        }

        if (this.method != PaymentMethod.POINT) {
            throw new CoreException(
                    ErrorType.BAD_REQUEST,
                    "Payment : 포인트 결제 방식이 아닙니다."
            );
        }

        // 포인트 차감 (포인트 부족 시 예외 발생)
        point.deduct(this.amount);

        // 결제 상태를 COMPLETED로 변경
        this.paymentStatus = PaymentStatus.COMPLETED;
    }

    @Override
    protected void guard() {
        if (method == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Payment : method가 비어있을 수 없습니다.");
        }

        if (amount == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Payment : amount가 비어있을 수 없습니다.");
        } else if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Payment : amount는 음수가 될 수 없습니다.");
        }

        if (paymentStatus == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Payment : paymentStatus가 비어있을 수 없습니다.");
        }

        // order는 생성 시점에는 null일 수 있음 (Payment.create()에서 설정)
        // guard에서는 검증하지 않음
    }
}

