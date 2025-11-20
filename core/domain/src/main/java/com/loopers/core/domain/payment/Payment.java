package com.loopers.core.domain.payment;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.DeletedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.order.vo.OrderId;
import com.loopers.core.domain.payment.vo.PayAmount;
import com.loopers.core.domain.payment.vo.PaymentId;
import com.loopers.core.domain.user.vo.UserId;
import lombok.Getter;

@Getter
public class Payment {

    private final PaymentId id;

    private final OrderId orderId;

    private final UserId userId;

    private final PayAmount amount;

    private final CreatedAt createdAt;

    private final UpdatedAt updatedAt;

    private final DeletedAt deletedAt;

    private Payment(
            PaymentId id,
            OrderId orderId,
            UserId userId,
            PayAmount amount,
            CreatedAt createdAt,
            UpdatedAt updatedAt,
            DeletedAt deletedAt
    ) {
        this.id = id;
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static Payment mappedBy(
            PaymentId id,
            OrderId orderId,
            UserId userId,
            PayAmount amount,
            CreatedAt createdAt,
            UpdatedAt updatedAt,
            DeletedAt deletedAt
    ) {
        return new Payment(
                id,
                orderId,
                userId,
                amount,
                createdAt,
                updatedAt,
                deletedAt
        );
    }

    public static Payment create(
            OrderId orderId,
            UserId userId,
            PayAmount amount
    ) {
        return new Payment(
                PaymentId.empty(),
                orderId,
                userId,
                amount,
                CreatedAt.now(),
                UpdatedAt.now(),
                DeletedAt.empty()
        );
    }
}
