package com.loopers.domain.payment;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "commerce_payment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class CommercePayment extends BaseEntity {

    // @Column(unique = true)
    private String transactionKey;
    
    @Enumerated(EnumType.STRING)
    private PaymentDto.PaymentMethod method;

    @Enumerated(EnumType.STRING)
    private PaymentDto.CardType cardType;

    private String cardNo;
    
    private BigDecimal amount;
    
    @Enumerated(EnumType.STRING)
    private PaymentDto.PaymentStatus paymentStatus = PaymentDto.PaymentStatus.PENDING;

    private String message;

    @Column(unique = true)
    private Long orderId;

    @Column(name = "last_event_occurred_at")
    private LocalDateTime lastEventOccurredAt;

    @Builder
    private CommercePayment(
            String transactionKey,
            PaymentDto.PaymentMethod method,
            PaymentDto.CardType cardType,
            String cardNo,
            PaymentDto.PaymentStatus paymentStatus,
            Long orderId,
            BigDecimal amount
    ) {
        this.transactionKey = transactionKey;
        this.method = method;
        this.cardType = cardType;
        this.cardNo = cardNo;
        this.paymentStatus = (paymentStatus != null) ? paymentStatus : PaymentDto.PaymentStatus.PENDING;
        this.orderId = orderId;
        this.amount = amount;
        guard();
    }

    @Override
    protected void guard() {
        // transactionKey 검증: null이 아니어야 함
        // if (transactionKey == null) {
        //     throw new CoreException(ErrorType.BAD_REQUEST, "Payment : transactionKey가 비어있을 수 없습니다.");
        // }

        // method 검증: null이 아니어야 함
        if (method == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Payment : method가 비어있을 수 없습니다.");
        }

        // orderId 검증: null이 아니어야 함 (결제 대상 주문 정보 필수)
        if (orderId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Payment : orderId가 비어있을 수 없습니다.");
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
    }

    public void success() {
        this.paymentStatus = PaymentDto.PaymentStatus.SUCCESS;
    }

    public void fail(String message) {
        this.paymentStatus = PaymentDto.PaymentStatus.FAILED;
        this.message = message;
    }

    /**
     * 마지막 처리된 이벤트 시각 업데이트
     */
    public void updateLastEventOccurredAt(LocalDateTime occurredAt) {
        this.lastEventOccurredAt = occurredAt;
    }
}

