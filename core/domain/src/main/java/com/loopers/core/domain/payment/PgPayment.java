package com.loopers.core.domain.payment;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.DeletedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.payment.type.PgPaymentStatus;
import com.loopers.core.domain.payment.vo.FailedReason;
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

    private final FailedReason failedReason;

    private final CreatedAt createdAt;

    private final UpdatedAt updatedAt;

    private final DeletedAt deletedAt;

    @Builder(access = AccessLevel.PRIVATE, toBuilder = true)
    private PgPayment(
            PgPaymentId id,
            PaymentId paymentId,
            TransactionKey transactionKey,
            PgPaymentStatus status,
            FailedReason failedReason,
            CreatedAt createdAt,
            UpdatedAt updatedAt,
            DeletedAt deletedAt
    ) {
        this.id = id;
        this.paymentId = paymentId;
        this.transactionKey = transactionKey;
        this.status = status;
        this.failedReason = failedReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static PgPayment create(
            TransactionKey transactionKey,
            PgPaymentStatus status,
            FailedReason failedReason
    ) {
        return new PgPayment(
                PgPaymentId.empty(),
                PaymentId.empty(),
                transactionKey,
                status,
                failedReason,
                CreatedAt.now(),
                UpdatedAt.now(),
                DeletedAt.empty()
        );
    }

    public static PgPayment mappedBy(
            PgPaymentId id,
            PaymentId paymentId,
            TransactionKey transactionKey,
            PgPaymentStatus status,
            FailedReason failedReason,
            CreatedAt createdAt,
            UpdatedAt updatedAt,
            DeletedAt deletedAt
    ) {
        return new PgPayment(id, paymentId, transactionKey, status, failedReason, createdAt, updatedAt, deletedAt);
    }

    public PgPayment with(
            PgPaymentStatus status,
            FailedReason failedReason
    ) {
        return this.toBuilder()
                .status(status)
                .failedReason(failedReason)
                .updatedAt(UpdatedAt.now())
                .build();
    }

    public PgPayment with(PaymentId paymentId) {
        return this.toBuilder()
                .paymentId(paymentId)
                .updatedAt(UpdatedAt.now())
                .build();
    }

    public PgPayment merge(PgPayment pgPayment) {
        return this.toBuilder()
                .transactionKey(pgPayment.transactionKey)
                .status(pgPayment.status)
                .failedReason(pgPayment.failedReason)
                .updatedAt(UpdatedAt.now())
                .build();
    }
}
