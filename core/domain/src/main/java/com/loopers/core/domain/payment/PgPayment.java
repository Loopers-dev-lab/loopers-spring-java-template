package com.loopers.core.domain.payment;

import com.loopers.core.domain.payment.type.PaymentStatus;
import com.loopers.core.domain.payment.vo.FailedReason;
import com.loopers.core.domain.payment.vo.TransactionKey;

public record PgPayment(
        TransactionKey transactionKey,
        PaymentStatus status,
        FailedReason failedReason
) {
}
