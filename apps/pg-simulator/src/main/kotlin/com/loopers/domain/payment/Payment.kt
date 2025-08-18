package com.loopers.domain.payment

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(
    name = "payments",
    indexes = [
        Index(name = "idx_user_transaction", columnList = "user_id, transaction_key"),
        Index(name = "idx_user_order", columnList = "user_id, order_id"),
        Index(name = "idx_unique_user_order_transaction", columnList = "user_id, order_id, transaction_key", unique = true),
    ]
)
class Payment(
    @Id
    @Column(name = "transaction_key", nullable = false, unique = true)
    val transactionKey: String,

    @Column(name = "user_id", nullable = false)
    val userId: String,

    @Column(name = "order_id", nullable = false)
    val orderId: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "card_type", nullable = false)
    val cardType: CardType,

    @Column(name = "card_no", nullable = false)
    val cardNo: String,

    @Column(name = "amount", nullable = false)
    val amount: Long,

    @Column(name = "callback_url", nullable = false)
    val callbackUrl: String,
) {
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: TransactionStatus = TransactionStatus.PENDING
        private set

    @Column(name = "reason", nullable = true)
    var reason: String? = null
        private set

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
        private set

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
        private set

    fun approve() {
        if (status != TransactionStatus.PENDING) {
            throw CoreException(ErrorType.INTERNAL_ERROR, "결제승인은 대기상태에서만 가능합니다.")
        }
        status = TransactionStatus.SUCCESS
        reason = "정상 승인되었습니다."
    }

    fun invalidCard() {
        if (status != TransactionStatus.PENDING) {
            throw CoreException(ErrorType.INTERNAL_ERROR, "결제처리는 대기상태에서만 가능합니다.")
        }
        status = TransactionStatus.FAILED
        reason = "잘못된 카드입니다. 다른 카드를 선택해주세요."
    }

    fun limitExceeded() {
        if (status != TransactionStatus.PENDING) {
            throw CoreException(ErrorType.INTERNAL_ERROR, "한도초과 처리는 대기상태에서만 가능합니다.")
        }
        status = TransactionStatus.FAILED
        reason = "한도초과입니다. 다른 카드를 선택해주세요."
    }
}
