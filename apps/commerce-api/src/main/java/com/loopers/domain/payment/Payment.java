package com.loopers.domain.payment;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "payments",
        indexes = {
                @Index(name = "idx_transaction_id", columnList = "transaction_id", unique = true),
                @Index(name = "idx_order_id", columnList = "order_id"),
                @Index(name = "idx_status_created", columnList = "status, created_at DESC")
        }
)
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Embedded
    private CardInfo cardInfo;

    @Embedded
    private PaymentAmount amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "transaction_id", length = 100, unique = true)
    private String transactionId;

    @Column(name = "callback_url", length = 500)
    private String callbackUrl;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "idempotency_key", length = 100, unique = true)
    private String idempotencyKey;

    // 포인트 결제 생성자
    private Payment(Long orderId, Long userId, PaymentMethod paymentMethod, Long amount, String idempotencyKey) {
        validateRequiredFieldsForPoint(orderId, userId, amount);
        validateIdempotencyKey(idempotencyKey);

        this.orderId = orderId;
        this.userId = userId;
        this.paymentMethod = paymentMethod;
        this.amount = PaymentAmount.of(amount);
        this.status = PaymentStatus.SUCCESS;
        this.cardInfo = null;
        this.callbackUrl = null;
        this.idempotencyKey = idempotencyKey;
    }

    // PG 카드 결제 생성자
    private Payment(Long orderId, Long userId, PaymentMethod paymentMethod,
                    CardInfo cardInfo, Long amount, String callbackUrl, String idempotencyKey) {
        validateRequiredFieldsForPgCard(orderId, userId, cardInfo, callbackUrl);
        validateIdempotencyKey(idempotencyKey);

        this.orderId = orderId;
        this.userId = userId;
        this.paymentMethod = paymentMethod;
        this.cardInfo = cardInfo;
        this.amount = PaymentAmount.of(amount);
        this.status = PaymentStatus.PENDING;
        this.callbackUrl = callbackUrl;
        this.idempotencyKey = idempotencyKey;
    }

    public static Payment createForPoint(Long orderId, Long userId, Long amount, String idempotencyKey) {
        return new Payment(orderId, userId, PaymentMethod.POINT, amount, idempotencyKey);
    }

    public static Payment createForPgCard(Long orderId, Long userId,
                                          String cardType, String cardNo,
                                          Long amount, String callbackUrl,
                                          String idempotencyKey) {
        return new Payment(
                orderId,
                userId,
                PaymentMethod.PG_CARD,
                CardInfo.of(cardType, cardNo),
                amount,
                callbackUrl,
                idempotencyKey
        );
    }

    private void validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                    "멱등성 키(Idempotency-Key)는 필수입니다.");
        }

        if (idempotencyKey.length() > 100) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                    "멱등성 키는 100자를 초과할 수 없습니다.");
        }

        if (!idempotencyKey.matches("^[a-zA-Z0-9-_:]+$")) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                    "멱등성 키는 영문, 숫자, -, _, : 만 사용할 수 있습니다.");
        }
    }

    private void validateRequiredFieldsForPoint(Long orderId, Long userId, Long amount) {
        if (orderId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 ID는 필수입니다.");
        }
        if (userId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자 ID는 필수입니다.");
        }
        if (amount == null || amount <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "결제 금액은 0보다 커야 합니다.");
        }
    }

    private void validateRequiredFieldsForPgCard(Long orderId, Long userId,
                                                 CardInfo cardInfo, String callbackUrl) {
        if (orderId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 ID는 필수입니다.");
        }
        if (userId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자 ID는 필수입니다.");
        }
        if (cardInfo == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "카드 정보는 필수입니다.");
        }
        if (callbackUrl == null || callbackUrl.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "콜백 URL은 필수입니다.");
        }
    }

    public void updateTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public void markAsSuccess() {
        if (this.status == PaymentStatus.SUCCESS) {
            return;
        }
        this.status = PaymentStatus.SUCCESS;
    }

    public void markAsFailed(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
    }

    public void markAsLimitExceeded() {
        this.status = PaymentStatus.LIMIT_EXCEEDED;
        this.failureReason = "카드 한도 초과";
    }

    public void markAsInvalidCard() {
        this.status = PaymentStatus.INVALID_CARD;
        this.failureReason = "잘못된 카드 정보";
    }

    public void markAsTimeout() {
        this.status = PaymentStatus.PG_TIMEOUT;
        this.failureReason = "PG 결제 요청 시간 초과";
    }

    public void markAsCallbackTimeout() {
        this.status = PaymentStatus.CALLBACK_TIMEOUT;
        this.failureReason = "결제 콜백 미수신 (30초 초과)";
    }

    public boolean isSuccess() {
        return this.status == PaymentStatus.SUCCESS;
    }

    public boolean isPending() {
        return this.status == PaymentStatus.PENDING;
    }

    public boolean isFailed() {
        return this.status == PaymentStatus.FAILED ||
                this.status == PaymentStatus.LIMIT_EXCEEDED ||
                this.status == PaymentStatus.INVALID_CARD ||
                this.status == PaymentStatus.PG_TIMEOUT ||
                this.status == PaymentStatus.CALLBACK_TIMEOUT;
    }

    public String getCardType() {
        return this.cardInfo.getCardType();
    }

    public String getCardNo() {
        return this.cardInfo.getCardNo();
    }

    public Long getAmountValue() {
        return this.amount.getValue();
    }
}
