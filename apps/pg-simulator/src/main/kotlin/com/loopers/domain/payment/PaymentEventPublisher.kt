package com.loopers.domain.payment

interface PaymentEventPublisher {
    fun publish(event: PaymentEvent.PaymentCreated)
    fun publish(event: PaymentEvent.PaymentHandled)
}
