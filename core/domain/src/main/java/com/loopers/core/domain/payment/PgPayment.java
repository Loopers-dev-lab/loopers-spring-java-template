package com.loopers.core.domain.payment;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.DeletedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.payment.type.PgPaymentStatus;
import com.loopers.core.domain.payment.vo.PaymentId;
import com.loopers.core.domain.payment.vo.PgPaymentId;
import com.loopers.core.domain.payment.vo.TransactionKey;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
public class PgPayment {

    private final PgPaymentId id;

    private final PaymentId paymentId;

    private final TransactionKey transactionKey;

    private final PgPaymentStatus status;

    private final CreatedAt createdAt;

    private final UpdatedAt updatedAt;

    private final DeletedAt deletedAt;

    @Builder(access = AccessLevel.PRIVATE, toBuilder = true)
    private PgPayment(
            PgPaymentId id,
            PaymentId paymentId,
            TransactionKey transactionKey,
            PgPaymentStatus status,
            CreatedAt createdAt,
            UpdatedAt updatedAt,
            DeletedAt deletedAt
    ) {
        this.id = id;
        this.paymentId = paymentId;
        this.transactionKey = transactionKey;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static PgPayment create(
            TransactionKey transactionKey,
            PgPaymentStatus status,
            CreatedAt createdAt,
            UpdatedAt updatedAt,
            DeletedAt deletedAt
    ) {
        return new PgPayment(
                PgPaymentId.empty(),
                PaymentId.empty(),
                transactionKey,
                status,
                createdAt,
                updatedAt,
                deletedAt
        );
    }

    public static PgPayment mappedBy(
            PgPaymentId id,
            PaymentId paymentId,
            TransactionKey transactionKey,
            PgPaymentStatus status,
            CreatedAt createdAt,
            UpdatedAt updatedAt,
            DeletedAt deletedAt
    ) {
        return new PgPayment(id, paymentId, transactionKey, status, createdAt, updatedAt, deletedAt);
    }

    public PgPayment with(
            PgPaymentStatus status
    ) {
        return this.toBuilder()
                .status(status)
                .updatedAt(UpdatedAt.now())
                .build();
    }
}
