package com.loopers.core.domain.payment.vo;

import com.loopers.core.domain.payment.type.PaymentType;

public record PaymentCreatedEvent(PaymentId paymentId, PaymentType paymentType) {

}
