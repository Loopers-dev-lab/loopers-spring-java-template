package com.loopers.application.payment

import com.loopers.domain.payment.Payment
import com.loopers.domain.payment.PaymentEvent
import com.loopers.domain.payment.PaymentEventPublisher
import com.loopers.domain.payment.PaymentRelay
import com.loopers.domain.payment.PaymentRepository
import com.loopers.domain.payment.TransactionKeyGenerator
import com.loopers.domain.user.UserInfo
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class PaymentApplicationService(
    private val paymentRepository: PaymentRepository,
    private val paymentEventPublisher: PaymentEventPublisher,
    private val paymentRelay: PaymentRelay,
    private val transactionKeyGenerator: TransactionKeyGenerator,
) {
    companion object {
        private val RATE_LIMIT_EXCEEDED = (1..20)
        private val RATE_INVALID_CARD = (21..30)
    }

    @Transactional
    fun createTransaction(command: PaymentCommand.CreateTransaction): TransactionInfo {
        command.validate()

        val transactionKey = transactionKeyGenerator.generate()
        val payment = paymentRepository.save(
            Payment(
                transactionKey = transactionKey,
                userId = command.userId,
                orderId = command.orderId,
                cardType = command.cardType,
                cardNo = command.cardNo,
                amount = command.amount,
                callbackUrl = command.callbackUrl,
            ),
        )

        paymentEventPublisher.publish(PaymentEvent.PaymentCreated.from(payment = payment))

        return TransactionInfo.from(payment)
    }

    @Transactional(readOnly = true)
    fun getTransactionDetailInfo(userInfo: UserInfo, transactionKey: String): TransactionInfo {
        val payment = paymentRepository.findByTransactionKey(userId = userInfo.userId, transactionKey = transactionKey)
            ?: throw CoreException(ErrorType.NOT_FOUND, "(transactionKey: $transactionKey) 결제건이 존재하지 않습니다.")
        return TransactionInfo.from(payment)
    }

    @Transactional(readOnly = true)
    fun findTransactionsByOrderId(userInfo: UserInfo, orderId: String): OrderInfo {
        val payments = paymentRepository.findByOrderId(userId = userInfo.userId, orderId = orderId)
        if (payments.isEmpty()) {
            throw CoreException(ErrorType.NOT_FOUND, "(orderId: $orderId) 에 해당하는 결제건이 존재하지 않습니다.")
        }

        return OrderInfo(
            orderId = orderId,
            transactions = payments.map { TransactionInfo.from(it) },
        )
    }

    @Transactional
    fun handle(transactionKey: String) {
        val payment = paymentRepository.findByTransactionKey(transactionKey)
            ?: throw CoreException(ErrorType.NOT_FOUND, "(transactionKey: $transactionKey) 결제건이 존재하지 않습니다.")

        val rate = (1..100).random()
        when (rate) {
            in RATE_LIMIT_EXCEEDED -> payment.limitExceeded()
            in RATE_INVALID_CARD -> payment.invalidCard()
            else -> payment.approve()
        }
        paymentEventPublisher.publish(event = PaymentEvent.PaymentHandled.from(payment))
    }

    fun notifyTransactionResult(transactionKey: String) {
        val payment = paymentRepository.findByTransactionKey(transactionKey)
            ?: throw CoreException(ErrorType.NOT_FOUND, "(transactionKey: $transactionKey) 결제건이 존재하지 않습니다.")
        paymentRelay.notify(callbackUrl = payment.callbackUrl, transactionInfo = TransactionInfo.from(payment))
    }
}
