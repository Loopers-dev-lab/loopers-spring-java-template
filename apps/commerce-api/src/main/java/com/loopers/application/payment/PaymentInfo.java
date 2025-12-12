package com.loopers.application.payment;

import com.loopers.domain.payment.CardType;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment 정보 DTO
 */
@Getter
@AllArgsConstructor
public class PaymentInfo {

    private final Long id;
    private final String userId;
    private final Long orderId;
    private final String pgTransactionKey;
    private final CardType cardType;
    private final String maskedCardNo;
    private final BigDecimal amount;
    private final PaymentStatus status;
    private final String failureReason;
    private final LocalDateTime createdAt;
    private final LocalDateTime modifiedAt;

    /**
     * Domain 객체로부터 생성
     */
    public static PaymentInfo from(Payment payment) {
        return new PaymentInfo(
                payment.getId(),
                payment.getUserId(),
                payment.getOrderId(),
                payment.getPgTransactionKey(),
                payment.getCardType(),
                payment.getMaskedCardNo(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getFailureReason(),
                payment.getCreatedAt(),
                payment.getModifiedAt()
        );
    }
}
