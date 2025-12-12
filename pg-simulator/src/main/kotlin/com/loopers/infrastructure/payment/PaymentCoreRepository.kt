package com.loopers.infrastructure.payment

import com.loopers.domain.payment.Payment
import com.loopers.domain.payment.PaymentRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import kotlin.jvm.optionals.getOrNull

@Component
class PaymentCoreRepository(
    private val paymentJpaRepository: PaymentJpaRepository,
) : PaymentRepository {
    @Transactional
    override fun save(payment: Payment): Payment {
        return paymentJpaRepository.save(payment)
    }

    @Transactional(readOnly = true)
    override fun findByTransactionKey(transactionKey: String): Payment? {
        return paymentJpaRepository.findById(transactionKey).getOrNull()
    }

    @Transactional(readOnly = true)
    override fun findByTransactionKey(userId: String, transactionKey: String): Payment? {
        return paymentJpaRepository.findByUserIdAndTransactionKey(userId, transactionKey)
    }

    override fun findByOrderId(userId: String, orderId: String): List<Payment> {
        return paymentJpaRepository.findByUserIdAndOrderId(userId, orderId)
            .sortedByDescending { it.updatedAt }
    }
}
