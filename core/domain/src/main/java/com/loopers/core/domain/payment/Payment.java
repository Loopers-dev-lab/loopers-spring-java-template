package com.loopers.core.domain.payment;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.DeletedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.order.vo.OrderKey;
import com.loopers.core.domain.payment.type.PaymentStatus;
import com.loopers.core.domain.payment.vo.*;
import com.loopers.core.domain.user.vo.UserId;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
public class Payment {

    private final PaymentId id;

    private final OrderKey orderKey;

    private final UserId userId;

    private final CardType cardType;

    private final CardNo cardNo;

    private final PayAmount amount;

    private final PaymentStatus status;

    private final TransactionKey transactionKey;

    private final FailedReason failedReason;

    private final CreatedAt createdAt;

    private final UpdatedAt updatedAt;

    private final DeletedAt deletedAt;

    @Builder(access = AccessLevel.PRIVATE, toBuilder = true)
    private Payment(
            PaymentId id,
            OrderKey orderKey,
            UserId userId,
            CardType cardType,
            CardNo cardNo,
            PayAmount amount,
            PaymentStatus status,
            TransactionKey transactionKey,
            FailedReason failedReason,
            CreatedAt createdAt,
            UpdatedAt updatedAt,
            DeletedAt deletedAt
    ) {
        this.id = id;
        this.orderKey = orderKey;
        this.userId = userId;
        this.cardType = cardType;
        this.cardNo = cardNo;
        this.amount = amount;
        this.status = status;
        this.transactionKey = transactionKey;
        this.failedReason = failedReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static Payment mappedBy(
            PaymentId id,
            OrderKey orderKey,
            UserId userId,
            CardType cardType,
            CardNo cardNo,
            PayAmount amount,
            PaymentStatus status,
            TransactionKey transactionKey,
            FailedReason failedReason,
            CreatedAt createdAt,
            UpdatedAt updatedAt,
            DeletedAt deletedAt
    ) {
        return new Payment(
                id,
                orderKey,
                userId,
                cardType,
                cardNo,
                amount,
                status,
                transactionKey,
                failedReason,
                createdAt,
                updatedAt,
                deletedAt
        );
    }

    public static Payment create(
            OrderKey orderKey,
            UserId userId,
            CardType cardType,
            CardNo cardNo,
            PayAmount amount
    ) {
        return new Payment(
                PaymentId.empty(),
                orderKey,
                userId,
                cardType,
                cardNo,
                amount,
                PaymentStatus.PENDING,
                TransactionKey.empty(),
                FailedReason.empty(),
                CreatedAt.now(),
                UpdatedAt.now(),
                DeletedAt.empty()
        );
    }

    public Payment withTransactionKey(TransactionKey transactionKey) {
        return this.toBuilder()
                .transactionKey(transactionKey)
                .updatedAt(UpdatedAt.now())
                .build();
    }

    public Payment withStatus(
            PaymentStatus status
    ) {
        return this.toBuilder()
                .status(status)
                .updatedAt(UpdatedAt.now())
                .build();
    }

    public Payment withFailedReason(FailedReason failedReason) {
        return this.toBuilder()
                .failedReason(failedReason)
                .updatedAt(UpdatedAt.now())
                .build();
    }

    public Payment fail(FailedReason failedReason) {
        return this.toBuilder()
                .status(PaymentStatus.FAILED)
                .failedReason(failedReason)
                .updatedAt(UpdatedAt.now())
                .build();
    }

    public Payment success() {
        return this.toBuilder()
                .status(PaymentStatus.SUCCESS)
                .updatedAt(UpdatedAt.now())
                .build();
    }
}
