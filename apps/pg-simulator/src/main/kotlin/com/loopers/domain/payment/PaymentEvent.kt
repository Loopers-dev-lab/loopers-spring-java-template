package com.loopers.domain.payment

object PaymentEvent {
    data class PaymentCreated(
        val transactionKey: String,
    ) {
        companion object {
            fun from(payment: Payment): PaymentCreated = PaymentCreated(transactionKey = payment.transactionKey)
        }
    }

    data class PaymentHandled(
        val transactionKey: String,
        val status: TransactionStatus,
        val reason: String?,
        val callbackUrl: String,
    ) {
        companion object {
            fun from(payment: Payment): PaymentHandled =
                PaymentHandled(
                    transactionKey = payment.transactionKey,
                    status = payment.status,
                    reason = payment.reason,
                    callbackUrl = payment.callbackUrl,
                )
        }
    }
}
