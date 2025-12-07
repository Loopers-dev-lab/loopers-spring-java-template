package com.loopers.core.service.payment.command;

public record PgCallbackCommand(
        String transactionKey,
        String orderId,
        String cardType,
        String cardNo,
        Long amount,
        String status,
        String reason
) {
    
}
