package com.loopers.domain.payment.event;

import com.loopers.domain.payment.CardType;
import com.loopers.domain.payment.PaymentType;
import lombok.Getter;

@Getter
public class CardPaymentRequestedEvent extends PaymentRequestedEvent{
    private final Long userId;
    private final CardType cardType;
    private final String cardNo;

    public CardPaymentRequestedEvent(
            Long orderId,
            Long userId,
            CardType cardType,
            String cardNo
    ) {
        super(orderId, PaymentType.CARD);
        this.userId = userId;
        this.cardType = cardType;
        this.cardNo = cardNo;
    }
}
