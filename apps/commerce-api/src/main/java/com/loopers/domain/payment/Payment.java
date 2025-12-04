package com.loopers.domain.payment;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "payments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String transactionKey;

    @Column(nullable = false, length = 50)
    private String orderId;

    @Column(nullable = false, length = 10)
    private String userId;

    @Column(nullable = false, precision = 19, scale = 0)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(length = 500)
    private String failureReason;

    @Column(nullable = false, length = 20)
    private String cardType;

    @Column(nullable = false, length = 19)
    private String cardNo;

    @Builder
    private Payment(String transactionKey, String orderId, String userId, BigDecimal amount,
        PaymentStatus status, String cardType, String cardNo) {
        validateTransactionKey(transactionKey);
        validateOrderId(orderId);
        validateUserId(userId);
        validateAmount(amount);

        this.transactionKey = transactionKey;
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.status = status != null ? status : PaymentStatus.PENDING;
        this.cardType = cardType;
        this.cardNo = cardNo;
    }

    private void validateTransactionKey(String transactionKey) {
        if (transactionKey == null || transactionKey.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Transaction Key는 필수입니다.");
        }
    }

    private void validateOrderId(String orderId) {
        if (orderId == null || orderId.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Order ID는 필수입니다.");
        }
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "User ID는 필수입니다.");
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "결제 금액은 0보다 커야 합니다.");
        }
    }

    public void updateStatus(PaymentStatus status, String failureReason) {
        this.status = status;
        if (status == PaymentStatus.FAILED && failureReason != null) {
            this.failureReason = failureReason;
        }
    }

    public boolean isPending() {
        return this.status == PaymentStatus.PENDING;
    }

    public boolean isSuccess() {
        return this.status == PaymentStatus.SUCCESS;
    }

    public boolean isFailed() {
        return this.status == PaymentStatus.FAILED;
    }
}
