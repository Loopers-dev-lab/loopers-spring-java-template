package com.loopers.core.infra.database.mysql.payment.entity;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.DeletedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.order.vo.OrderKey;
import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.type.PaymentStatus;
import com.loopers.core.domain.payment.vo.*;
import com.loopers.core.domain.user.vo.UserId;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Entity
@Table(
        name = "payments",
        indexes = {
                @Index(name = "idx_payment_order_key", columnList = "order_key"),
                @Index(name = "idx_payment_transaction_key", columnList = "transaction_key"),
                @Index(name = "idx_payment_user_id", columnList = "user_id"),
                @Index(name = "idx_payment_created_at", columnList = "created_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String orderKey;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String cardType;

    @Column(nullable = false)
    private String cardNo;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String paymentStatus;

    @Lob
    private String failedReason;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    public static PaymentEntity from(Payment payment) {
        return new PaymentEntity(
                Optional.ofNullable(payment.getId().value())
                        .map(Long::parseLong)
                        .orElse(null),
                payment.getOrderKey().value(),
                Long.parseLong(Objects.requireNonNull(payment.getUserId().value())),
                payment.getCardType().value(),
                payment.getCardNo().value(),
                payment.getAmount().value(),
                payment.getStatus().name(),
                payment.getFailedReason().value(),
                payment.getCreatedAt().value(),
                payment.getUpdatedAt().value(),
                payment.getDeletedAt().value()
        );
    }

    public Payment to() {
        return Payment.mappedBy(
                new PaymentId(this.id.toString()),
                new OrderKey(this.orderKey),
                new UserId(this.userId.toString()),
                new CardType(this.cardType),
                new CardNo(this.cardNo),
                new PayAmount(this.amount),
                PaymentStatus.valueOf(this.paymentStatus),
                new FailedReason(failedReason),
                new CreatedAt(this.createdAt),
                new UpdatedAt(this.updatedAt),
                new DeletedAt(this.deletedAt)
        );
    }
}
