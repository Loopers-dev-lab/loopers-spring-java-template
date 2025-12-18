package com.loopers.interfaces.api.payment;

import com.loopers.application.payment.TransactionInfo;
import com.loopers.domain.payment.CardType;
import com.loopers.domain.payment.TransactionStatus;

public class PaymentV1Dto {
    public record PaymentCallbackRequest(
            String transactionKey,
            CardType cardType,
            String cardNo,
            Long amount,
            TransactionStatus status,
            String reason
    ) {

        public TransactionInfo toTransactionInfo() {
            return new TransactionInfo(
                    transactionKey,
                    cardType,
                    cardNo,
                    amount,
                    status,
                    reason
            );
        }
    }
}
