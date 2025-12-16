package com.loopers.domain.payment.event;

import com.loopers.domain.payment.PaymentType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class PaymentRequestedEvent {
    private final Long orderId;
    private final PaymentType paymentType;
}
