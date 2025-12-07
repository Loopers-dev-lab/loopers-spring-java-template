package com.loopers.core.infra.database.mysql.payment.entity;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.DeletedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.payment.PgPayment;
import com.loopers.core.domain.payment.type.PgPaymentStatus;
import com.loopers.core.domain.payment.vo.PaymentId;
import com.loopers.core.domain.payment.vo.PgPaymentId;
import com.loopers.core.domain.payment.vo.TransactionKey;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Optional;

@Entity
@Table(
        name = "payments",
        indexes = {
                @Index(name = "idx_payment_order_key", columnList = "order_key"),
                @Index(name = "idx_payment_transaction_key", columnList = "transaction_key"),
                @Index(name = "idx_payment_created_at", columnList = "created_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PgPaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long paymentId;

    @Column(nullable = false)
    private String transactionKey;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    public static PgPaymentEntity from(PgPayment pgPayment) {
        return new PgPaymentEntity(
                Optional.ofNullable(pgPayment.getId().value())
                        .map(Long::parseLong)
                        .orElse(null),
                Long.parseLong(pgPayment.getPaymentId().value()),
                pgPayment.getTransactionKey().value(),
                pgPayment.getStatus().name(),
                pgPayment.getCreatedAt().value(),
                pgPayment.getUpdatedAt().value(),
                pgPayment.getDeletedAt().value()
        );
    }

    public PgPayment to() {
        return PgPayment.mappedBy(
                new PgPaymentId(this.id.toString()),
                new PaymentId(this.paymentId.toString()),
                new TransactionKey(this.transactionKey),
                PgPaymentStatus.valueOf(this.status),
                new CreatedAt(this.createdAt),
                new UpdatedAt(this.updatedAt),
                new DeletedAt(this.deletedAt)
        );
    }
}
