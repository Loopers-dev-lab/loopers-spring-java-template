package com.loopers.application.payment;

import com.loopers.domain.payment.CardType;
import com.loopers.domain.payment.TransactionStatus;

public record TransactionInfo(
        String transactionKey,
        CardType cardType,
        String cardNo,
        Long amount,
        TransactionStatus status,
        String reason
) {
}
