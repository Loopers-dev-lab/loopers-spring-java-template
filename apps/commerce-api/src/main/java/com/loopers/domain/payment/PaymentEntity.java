package com.loopers.domain.payment;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Objects;

import com.loopers.application.payment.PaymentCommand;
import com.loopers.domain.BaseEntity;
import com.loopers.domain.payment.event.PaymentCompletedEvent;
import com.loopers.domain.payment.event.PaymentFailedEvent;
import com.loopers.domain.payment.event.PaymentTimeoutEvent;
import com.loopers.domain.user.UserEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.util.MaskingUtil;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

/**
 * @author hyunjikoh
 * @since 2025. 12. 2.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "loopers_payments", indexes = {
        @Index(name = "idx_payment_user_id", columnList = "user_id"),
        @Index(name = "idx_payment_transaction_key", columnList = "transaction_key"),
        @Index(name = "idx_payment_order_id", columnList = "orer_id"),
})
public class PaymentEntity extends BaseEntity {

    @Column(nullable = true, unique = true, length = 50, name = "transaction_key")
    private String transactionKey;

    @Column(nullable = false, name = "order_id")
    private Long orderNumber;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, precision = 10, scale = 2, name = "amount")
    private BigDecimal amount;

    @Column(nullable = false, length = 20, name = "card_type")
    private String cardType;

    @Column(nullable = false, length = 20, name = "card_no")
    private String cardNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, name = "payment_status")
    private PaymentStatus paymentStatus;

    @Column(nullable = false, length = 255, name = "callback_url")
    private String callbackUrl;

    @Column(columnDefinition = "TEXT", name = "failure_reason")
    private String failureReason;

    @Column(name = "completed_at")
    private ZonedDateTime completedAt;

    // → PG에 실제 결제 요청을 보낸 시각
    @Column(name = "requested_at", nullable = false)
    private ZonedDateTime requestedAt;

    /**
     * 결제 생성 팩토리 메서드
     *
     * @param request
     * @return
     */
    public static PaymentEntity createPayment(PaymentDomainDtos.PaymentDomainCreateRequest request) {
        Objects.requireNonNull(request, "결제 생성 요청은 null일 수 없습니다.");
        return new PaymentEntity(request);
    }


    private PaymentEntity(PaymentDomainDtos.PaymentDomainCreateRequest request) {

        // transactionKey는 PENDING 상태에서 null 허용 (명시적 주석)
        Objects.requireNonNull(request, "결제 생성 요청은 필수입니다.");
        Objects.requireNonNull(request.orderNumber(), "주문 ID는 필수입니다.");
        Objects.requireNonNull(request.amount(), "결제 금액은 필수입니다.");
        Objects.requireNonNull(request.cardType(), "카드 타입은 필수입니다.");
        Objects.requireNonNull(request.cardNo(), "카드 번호는 필수입니다.");
        Objects.requireNonNull(request.paymentStatus(), "결제 상태는 필수입니다.");
        Objects.requireNonNull(request.callbackUrl(), "콜백 URL은 필수입니다.");
        Objects.requireNonNull(request.requestedAt(), "요청 시각은 필수입니다.");

        if (request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("결제 금액은 0보다 커야 합니다.");
        }

        // transactionKey는 null 가능 (PENDING 생성 시)
        this.transactionKey = request.transactionKey();
        this.orderNumber = request.orderNumber();
        this.amount = request.amount();
        this.cardType = request.cardType();
        this.cardNo = MaskingUtil.maskCardNumber(request.cardNo());
        this.paymentStatus = request.paymentStatus();
        this.callbackUrl = request.callbackUrl();
        this.userId = request.userId();
        this.requestedAt = request.requestedAt();
        this.failureReason = request.failureReason();
    }

    public static PaymentEntity createPending(UserEntity user, PaymentCommand command) {
        PaymentDomainDtos.PaymentDomainCreateRequest request = new PaymentDomainDtos.PaymentDomainCreateRequest(
                user.getId(),
                command.orderNumber(),
                null,
                command.cardType(),
                command.cardNo(),
                command.callbackUrl(),
                command.amount(),
                PaymentStatus.PENDING,
                ZonedDateTime.now(),
                null
        );

        return createPayment(request);

    }

    public static PaymentEntity createFailed(UserEntity user, PaymentCommand command, String reason) {
        PaymentDomainDtos.PaymentDomainCreateRequest request = new PaymentDomainDtos.PaymentDomainCreateRequest(
                user.getId(),
                command.orderNumber(),
                null,
                command.cardType(),
                command.cardNo(),
                command.callbackUrl(),
                command.amount(),
                PaymentStatus.FAILED,
                ZonedDateTime.now(),
                reason
        );
        return createPayment(request);
    }

    public void updateTransactionKey(String transactionKey) {
        this.transactionKey = transactionKey;
    }

    /**
     * 결제 완료 처리
     */
    public void complete() {
        if (this.paymentStatus != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    String.format("PENDING 상태의 결제만 완료 처리할 수 있습니다. (현재 상태: %s)", this.paymentStatus)
            );
        }
        this.paymentStatus = PaymentStatus.COMPLETED;
        this.completedAt = ZonedDateTime.now();
    }

    /**
     * 결제 완료 처리 (도메인 이벤트 발행)
     */
    public void completeWithEvent() {
        complete();

        // 도메인 이벤트 발행 (이벤트 핸들러에서 데이터 플랫폼 연동 처리)
        registerEvent(new PaymentCompletedEvent(
                this.transactionKey,
                this.orderNumber,
                this.userId,
                this.amount,
                this.cardType
        ));
    }



    /**
     * 결제 실패 처리 (도메인 이벤트 발행)
     */
    public void fail(String reason) {

        if (this.paymentStatus != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    String.format("PENDING 상태의 결제만 실패 처리할 수 있습니다. (현재 상태: %s)", this.paymentStatus)
            );
        }
        this.failureReason = reason;
        this.paymentStatus = PaymentStatus.FAILED;

        // 도메인 이벤트 발행 (이벤트 핸들러에서 데이터 플랫폼 연동 처리)
        registerEvent(new PaymentFailedEvent(
                this.transactionKey,
                this.orderNumber,
                this.userId,
                this.amount,
                this.cardType,
                reason
        ));
    }

    /**
     * 결제 타임아웃 처리
     */
    public void timeout() {
        if (this.paymentStatus != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    String.format("PENDING 상태의 결제만 타임아웃 처리할 수 있습니다. (현재 상태: %s)", this.paymentStatus)
            );
        }
        this.failureReason = "결제 콜백 타임아웃 (10분 초과)";
        this.paymentStatus = PaymentStatus.TIMEOUT;
    }

    /**
     * 결제 타임아웃 처리 (도메인 이벤트 발행)
     */
    public void timeoutWithEvent() {
        timeout();

        // 도메인 이벤트 발행 (이벤트 핸들러에서 데이터 플랫폼 연동 처리)
        registerEvent(new PaymentTimeoutEvent(
                this.transactionKey,
                this.orderNumber,
                this.userId,
                this.amount,
                this.cardType
        ));
    }

    /**
     * 결제 취소 처리
     */
    public void cancel() {
        if (this.paymentStatus != PaymentStatus.COMPLETED) {
            throw new IllegalStateException(
                    String.format("완료된 결제만 취소할 수 있습니다. (현재 상태: %s)", this.paymentStatus)
            );
        }
        this.paymentStatus = PaymentStatus.CANCEL;
    }

    /**
     * 결제 환불 처리
     */
    public void refund() {
        if (this.paymentStatus != PaymentStatus.COMPLETED && this.paymentStatus != PaymentStatus.CANCEL) {
            throw new IllegalStateException(
                    String.format("완료 또는 취소된 결제만 환불할 수 있습니다. (현재 상태: %s)", this.paymentStatus)
            );
        }
        this.paymentStatus = PaymentStatus.REFUNDED;
    }

    /**
     * PG 콜백 결과 처리 (도메인 이벤트 발행)
     *
     * @param status PG로부터 받은 결제 상태 ("SUCCESS", "FAILED", "PENDING")
     * @param reason 실패 사유 (실패인 경우에만)
     */
    public void processCallbackResult(String status, String reason) {
        switch (status) {
            case "SUCCESS" -> {
                if (this.paymentStatus == PaymentStatus.COMPLETED)
                    return;
                completeWithEvent();

            }
            case "FAILED" -> {
                if (this.paymentStatus == PaymentStatus.FAILED)
                    return;
                fail(reason);
            }
            case "PENDING" -> {
                // PENDING 상태는 아직 처리 중이므로 아무 작업도 하지 않음
            }
            default -> throw new CoreException(ErrorType.INVALID_PAYMENT_STATUS);
        }
    }

    @Override
    protected void guard() {
        if (this.amount != null && this.amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("결제 금액은 0보다 커야 합니다.");
        }
        if (this.paymentStatus == null) {
            throw new IllegalStateException("결제 상태는 필수입니다.");
        }
    }
}
