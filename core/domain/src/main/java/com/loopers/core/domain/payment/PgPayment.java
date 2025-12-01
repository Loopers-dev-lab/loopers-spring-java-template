package com.loopers.core.domain.payment;

import com.loopers.core.domain.payment.vo.TransactionKey;

public record PgPayment(
        TransactionKey transactionKey
) {
}
