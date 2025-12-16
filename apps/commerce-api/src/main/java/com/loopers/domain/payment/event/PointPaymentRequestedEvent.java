package com.loopers.domain.payment.event;

import com.loopers.domain.payment.PaymentType;
import lombok.Getter;

@Getter
public class PointPaymentRequestedEvent extends PaymentRequestedEvent {
    private final Long userId;

    public PointPaymentRequestedEvent(Long orderId, Long userId) {
        super(orderId, PaymentType.POINT);
        this.userId = userId;
    }
}
