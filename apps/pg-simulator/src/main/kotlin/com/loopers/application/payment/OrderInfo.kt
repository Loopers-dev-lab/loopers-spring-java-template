package com.loopers.application.payment

/**
 * 결제 주문 정보
 *
 * 결제는 주문에 대한 다수 트랜잭션으로 구성됩니다.
 *
 * @property orderId 주문 정보
 * @property transactions 주문에 엮인 트랜잭션 목록
 */
data class OrderInfo(
    val orderId: String,
    val transactions: List<TransactionInfo>,
)
