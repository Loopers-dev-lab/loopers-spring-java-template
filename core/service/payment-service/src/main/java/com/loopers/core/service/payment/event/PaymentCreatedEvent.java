package com.loopers.core.service.payment.event;

import com.loopers.core.domain.payment.type.PaymentType;
import com.loopers.core.domain.payment.vo.PaymentId;

public record PaymentCreatedEvent(PaymentId paymentId, PaymentType paymentType) {

}
