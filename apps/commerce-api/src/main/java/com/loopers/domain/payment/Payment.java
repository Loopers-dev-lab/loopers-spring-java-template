package com.loopers.domain.payment;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Table(name = "payments")
@Entity
public class Payment extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", nullable = false)
    private CardType cardType;

    @Column(name = "card_no", nullable = false)
    private String cardNo;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "fail_reason", nullable = true)
    private String failReason;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "transaction_key", nullable = true)
    private String transactionKey;

    public static Payment create(final CardType cardType, final String cardNo, final Long amount, final Long orderId) {
        Payment payment = new Payment();
        payment.cardType = cardType;
        payment.cardNo = cardNo;
        payment.orderId = orderId;
        payment.amount = amount;
        payment.paymentStatus = PaymentStatus.PENDING;
        return payment;
    }

    public void success(String transactionKey) {
        this.transactionKey = transactionKey;
    }

    public void paid() {
        if (this.paymentStatus != PaymentStatus.PENDING) {
            throw new CoreException(ErrorType.INTERNAL_ERROR, "결제승인은 대기상태에서만 가능합니다.");
        }
        this.paymentStatus = PaymentStatus.PAID;
    }

    public void fail(String failReason) {
        if (this.paymentStatus != PaymentStatus.PENDING) {
            throw new CoreException(ErrorType.INTERNAL_ERROR, "결제실패는 대기상태에서만 가능합니다.");
        }
        this.paymentStatus = PaymentStatus.FAILED;
        this.failReason = failReason;
    }
}
