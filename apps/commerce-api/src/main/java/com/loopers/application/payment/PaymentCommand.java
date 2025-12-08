package com.loopers.application.payment;

import java.math.BigDecimal;

import lombok.Builder;

/**
 * 결제 명령 DTO
 *
 * @author hyunjikoh
 * @since 2025. 12. 4.
 */
@Builder
public record PaymentCommand(
        String username,
        Long orderId,
        Long orderNumber,
        String cardType,
        String cardNo,
        BigDecimal amount,
        String callbackUrl
) {
    @Override
    public String toString() {
        String maskedCardNo = cardNo != null && cardNo.length() >= 4
                ? "*".repeat(cardNo.length() - 4) + cardNo.substring(cardNo.length() - 4)
                : "****";
        return "PaymentCommand[username=%s, orderNumber=%d, orderNumber=%d, cardType=%s, cardNo=%s, amount=%s, callbackUrl=%s]"
                .formatted(username, orderId, orderNumber, cardType, maskedCardNo, amount, callbackUrl);
    }
}
