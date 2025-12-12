package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.CardType;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 JPA Entity
 */
@Entity
@Table(
        name = "payments",
        indexes = {
                @Index(name = "idx_user_order", columnList = "userId, orderId"),
                @Index(name = "idx_pg_transaction_key", columnList = "pgTransactionKey"),
                @Index(name = "idx_status", columnList = "status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private Long orderId;

    @Column(unique = true)
    private String pgTransactionKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CardType cardType;

    @Column(nullable = false)
    private String maskedCardNo;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(length = 500)
    private String failureReason;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime modifiedAt;

    // === 생성자 ===

    private PaymentEntity(
            String userId,
            Long orderId,
            String pgTransactionKey,
            CardType cardType,
            String maskedCardNo,
            BigDecimal amount,
            PaymentStatus status,
            String failureReason,
            LocalDateTime createdAt,
            LocalDateTime modifiedAt
    ) {
        this.userId = userId;
        this.orderId = orderId;
        this.pgTransactionKey = pgTransactionKey;
        this.cardType = cardType;
        this.maskedCardNo = maskedCardNo;
        this.amount = amount;
        this.status = status;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
        this.modifiedAt = modifiedAt;
    }

    // === Domain <-> Entity 변환 ===

    /**
     * Domain 객체로부터 Entity 생성
     */
    public static PaymentEntity from(Payment payment) {
        PaymentEntity entity = new PaymentEntity(
                payment.getUserId(),
                payment.getOrderId(),
                payment.getPgTransactionKey(),
                payment.getCardType(),
                payment.getMaskedCardNo(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getFailureReason(),
                payment.getCreatedAt(),
                payment.getModifiedAt()
        );
        entity.id = payment.getId();
        return entity;
    }

    /**
     * Entity로부터 Domain 객체 생성
     */
    public Payment toDomain() {
        return Payment.reconstruct(
                this.id,
                this.userId,
                this.orderId,
                this.pgTransactionKey,
                this.cardType,
                this.maskedCardNo,
                this.amount,
                this.status,
                this.failureReason,
                this.createdAt,
                this.modifiedAt
        );
    }

    /**
     * Domain 객체의 상태를 Entity에 반영
     */
    public void updateFromDomain(Payment payment) {
        this.pgTransactionKey = payment.getPgTransactionKey();
        this.status = payment.getStatus();
        this.failureReason = payment.getFailureReason();
        this.modifiedAt = payment.getModifiedAt();
    }
}
