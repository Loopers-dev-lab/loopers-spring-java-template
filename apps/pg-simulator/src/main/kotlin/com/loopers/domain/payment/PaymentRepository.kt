package com.loopers.domain.payment

interface PaymentRepository {
    fun save(payment: Payment): Payment
    fun findByTransactionKey(transactionKey: String): Payment?
    fun findByTransactionKey(userId: String, transactionKey: String): Payment?
    fun findByOrderId(userId: String, orderId: String): List<Payment>
}
