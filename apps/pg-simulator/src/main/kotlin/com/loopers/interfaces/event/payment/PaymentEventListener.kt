package com.loopers.interfaces.event.payment

import com.loopers.application.payment.PaymentApplicationService
import com.loopers.domain.payment.PaymentEvent
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class PaymentEventListener(
    private val paymentApplicationService: PaymentApplicationService,
) {
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: PaymentEvent.PaymentCreated) {
        val thresholdMillis = (1000L..5000L).random()
        Thread.sleep(thresholdMillis)

        paymentApplicationService.handle(event.transactionKey)
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: PaymentEvent.PaymentHandled) {
        paymentApplicationService.notifyTransactionResult(transactionKey = event.transactionKey)
    }
}
