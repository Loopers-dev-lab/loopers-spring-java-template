package com.loopers.application.payment

import com.loopers.domain.payment.CardType
import com.loopers.domain.payment.Payment
import com.loopers.domain.payment.TransactionStatus

/**
 * 트랜잭션 정보
 *
 * @property transactionKey 트랜잭션 KEY
 * @property orderId 주문 ID
 * @property cardType 카드 종류
 * @property cardNo 카드 번호
 * @property amount 금액
 * @property status 처리 상태
 * @property reason 처리 사유
 */
data class TransactionInfo(
    val transactionKey: String,
    val orderId: String,
    val cardType: CardType,
    val cardNo: String,
    val amount: Long,
    val status: TransactionStatus,
    val reason: String?,
) {
    companion object {
        fun from(payment: Payment): TransactionInfo =
            TransactionInfo(
                transactionKey = payment.transactionKey,
                orderId = payment.orderId,
                cardType = payment.cardType,
                cardNo = payment.cardNo,
                amount = payment.amount,
                status = payment.status,
                reason = payment.reason,
            )
    }
}
