package com.loopers.core.infra.database.mysql.payment.entity;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.DeletedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.order.vo.OrderId;
import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.vo.PayAmount;
import com.loopers.core.domain.payment.vo.PaymentId;
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
        @Index(name = "idx_payment_order_id", columnList = "order_id"),
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
    private Long orderId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private BigDecimal amount;

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
            Long.parseLong(Objects.requireNonNull(payment.getOrderId().value())),
            Long.parseLong(Objects.requireNonNull(payment.getUserId().value())),
            payment.getAmount().value(),
            payment.getCreatedAt().value(),
            payment.getUpdatedAt().value(),
            payment.getDeletedAt().value()
        );
    }

    public Payment to() {
        return Payment.mappedBy(
            new PaymentId(this.id.toString()),
            new OrderId(this.orderId.toString()),
            new UserId(this.userId.toString()),
            new PayAmount(this.amount),
            new CreatedAt(this.createdAt),
            new UpdatedAt(this.updatedAt),
            new DeletedAt(this.deletedAt)
        );
    }
}
