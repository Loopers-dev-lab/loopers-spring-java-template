package com.loopers.domain.payment;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Entity
@Table(name = "payment")
@Getter
public class Payment extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String orderKey;

    @Column(unique = true)
    private String transactionKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false)
    private Integer amount;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "cardType", column = @Column(name = "card_type", nullable = true)),
            @AttributeOverride(name = "cardNumber", column = @Column(name = "card_number", nullable = true))
    })
    private Card card;

    @Column
    private LocalDateTime paidAt;

    @Builder
    private Payment(String orderKey, String transactionKey, PaymentStatus status, Integer amount,
                    Card card, LocalDateTime paidAt) {
        validateCreatePayment(amount, orderKey);
        this.orderKey = orderKey;
        this.transactionKey = transactionKey;
        this.status = status;
        this.amount = amount;
        this.card = card;
        this.paidAt = paidAt;
    }

    public Payment() {
    }

    public static Payment createPayment(Integer amount, String orderKey) {
        return Payment.builder()
                .orderKey(orderKey)
                .status(PaymentStatus.PENDING)
                .amount(amount)
                .build();
    }

    private static void validateCreatePayment(Integer amount, String orderKey) {
        validateAmount(amount);
        validateOrderKey(orderKey);
    }

    private static void validateAmount(Integer amount) {
        if (amount == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "결제 금액은 필수입니다.");
        }
        if (amount <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "결제 금액은 0보다 커야 합니다.");
        }
    }

    private static void validateOrderKey(String orderKey) {
        if (orderKey == null || orderKey.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 키는 필수입니다.");
        }

        if (orderKey.length() < 6) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 키는 최소 6자리 이상이어야 합니다.");
        }
    }

    public void updateStatus(PaymentStatus status) {
        if (status == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "결제 상태는 필수입니다.");
        }
        this.status = status;
        if (status == PaymentStatus.COMPLETED) {
            this.paidAt = LocalDateTime.now();
        }
    }

    public void updateTransactionKey(String transactionKey) {
        if (transactionKey == null || transactionKey.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "거래 키는 필수입니다.");
        }
        this.transactionKey = transactionKey;
    }

    public void updateCard(Card card) {
        this.card = card;
    }

    public boolean isCompleted() {
        return this.status == PaymentStatus.COMPLETED;
    }

    public boolean isPending() {
        return this.status == PaymentStatus.PENDING;
    }

    public boolean isFailed() {
        return this.status == PaymentStatus.FAILED;
    }

    public boolean hasTransactionKey() {
        return this.transactionKey != null && !this.transactionKey.isBlank();
    }
}

