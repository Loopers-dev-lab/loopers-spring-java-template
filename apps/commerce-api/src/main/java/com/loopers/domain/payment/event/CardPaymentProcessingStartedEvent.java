package com.loopers.domain.payment.event;

import com.loopers.domain.payment.CardType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CardPaymentProcessingStartedEvent {
    private final String paymentId;
    private final Long orderId;
    private final String userId;
    private final CardType cardType;
    private final String cardNo;
}
