package com.loopers.domain.payment;

import com.loopers.domain.Money;
import com.loopers.domain.order.Order;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@Table(name = "payment")
@Getter
public class Payment {

    @Id
    @Column(name = "payment_id", nullable = false, updatable = false, length = 36)
    private String paymentId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", referencedColumnName = "id")
    private Order order;

    @Column(name = "pg_transaction_id", unique = true)
    private String pgTransactionId; // PG사에서 발급한 승인 번호

    @Embedded
    @Column(name = "amount", nullable = false)
    private Money amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false)
    private PaymentType paymentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "card_type")
    private CardType cardType;

    @Column(name = "card_no")
    private String cardNo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;    // 결제 생성 일시

    @Column(name = "completed_at")
    private LocalDateTime completedAt;  // 결제 완료 일시

    @Column(name = "status_check_count", nullable = false)
    private Integer statusCheckCount = 0;  // 상태 확인 횟수

    @Column(name = "last_status_check_at")
    private LocalDateTime lastStatusCheckAt;  // 마지막 상태 확인 시간

    @PrePersist
    private void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    public Payment(String paymentId, Order order, Money amount, PaymentType paymentType, CardType cardType, String cardNo) {
        validatePaymentType(paymentType, cardType, cardNo);

        this.paymentId = paymentId;
        this.order = order;
        this.amount = amount;
        this.paymentType = paymentType;
        this.cardType = cardType;
        this.cardNo = cardNo;
        this.status = PaymentStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.statusCheckCount = 0;
    }

    /**
     * 포인트 결제 생성
     * PaymentProcessor에서 사용
     */
    public static Payment createPointPayment(Order order, com.loopers.domain.user.User user) {
        return createPaymentForPoint(
                order,
                order.getTotalPrice(),
                PaymentType.POINT
        );
    }

    /**
     * 카드 결제 생성
     * PaymentProcessor에서 사용
     */
    public static Payment createCardPayment(Order order, CardType cardType, String cardNo) {
        return createPaymentForCard(
                order,
                order.getTotalPrice(),
                PaymentType.CARD,
                cardType, cardNo
        );
    }

    public static Payment createPaymentForPoint(Order order, Money amount, PaymentType paymentType) {
        String paymentKey = PaymentKeyGenerator.generateKeyForPoint();

        return Payment.builder()
                .paymentId(paymentKey)
                .order(order)
                .amount(amount)
                .paymentType(paymentType)
                .build();
    }


    public static Payment createPaymentForCard(Order order, Money amount, PaymentType paymentType, CardType cardType, String cardNo) {

        String paymentKey = PaymentKeyGenerator.generateKeyForCard();

        return Payment.builder()
                .paymentId(paymentKey)
                .order(order)
                .amount(amount)
                .paymentType(paymentType)
                .cardType(cardType)
                .cardNo(cardNo)
                .build();
    }

    public void startProcessing(String pgTransactionId) {
        if (this.status != PaymentStatus.PENDING) {
            throw new CoreException(ErrorType.BAD_REQUEST, "결제 처리를 시작할 수 없는 상태입니다");
        }
        this.pgTransactionId = pgTransactionId;
        this.status = PaymentStatus.PROCESSING;
    }

    /**
     * 포인트 결제 즉시 완료
     * PENDING → SUCCESS (동기식 결제)
     */
    public void completePointPayment() {
        if (this.status != PaymentStatus.PENDING) {
            throw new CoreException(ErrorType.BAD_REQUEST, "포인트 결제를 완료할 수 없는 상태입니다");
        }
        this.status = PaymentStatus.SUCCESS;
        this.completedAt = LocalDateTime.now();
    }

    /**
     * 카드 결제 완료
     * PROCESSING → SUCCESS (비동기 콜백)
     */
    public void completePayment() {
        if (this.status != PaymentStatus.PROCESSING) {
            throw new CoreException(ErrorType.BAD_REQUEST, "결제를 완료할 수 없는 상태입니다");
        }
        this.status = PaymentStatus.SUCCESS;
        this.completedAt = LocalDateTime.now();
    }

    public void failPayment(String reason) {
        if (this.status != PaymentStatus.PROCESSING) {
            throw new CoreException(ErrorType.BAD_REQUEST, "결제를 실패 처리할 수 없는 상태입니다 : " + reason);
        }
        this.status = PaymentStatus.FAILED;
    }

    /**
     * 상태 확인 횟수 증가
     */
    public void incrementStatusCheckCount() {
        this.statusCheckCount++;
        this.lastStatusCheckAt = LocalDateTime.now();
    }

    /**
     * 상태 확인이 가능한지 확인
     * @param maxCheckCount 최대 확인 횟수
     * @return 확인 가능 여부
     */
    public boolean canCheckStatus(int maxCheckCount) {
        return this.statusCheckCount < maxCheckCount;
    }

    /**
     * PROCESSING 상태가 일정 시간 이상 지속되었는지 확인
     * @param minutes 경과 시간 (분)
     * @return 경과 여부
     */
    public boolean isProcessingOverMinutes(int minutes) {
        if (this.status != PaymentStatus.PROCESSING) {
            return false;
        }
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(minutes);

        // 마지막 상태 확인이 있으면 그 시간 기준, 없으면 생성 시간 기준
        LocalDateTime baseTime = this.lastStatusCheckAt != null ? this.lastStatusCheckAt : this.createdAt;
        return baseTime.isBefore(threshold);
    }

    private void validatePaymentType(PaymentType paymentType, CardType cardType, String cardNo) {
        if (paymentType == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "결제 방식은 필수값입니다");
        }

        if (paymentType == PaymentType.CARD) {
            validateCardNo(cardNo);
            validateCardType(cardType);
        }
    }

    private void validateCardNo(String cardNo) {
        if (cardNo == null || cardNo.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "카드 번호는 필수값입니다");
        }
    }

    private void validateCardType(CardType cardType) {
        if (cardType == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "카드사 정보는 필수값입니다");
        }
    }

}
