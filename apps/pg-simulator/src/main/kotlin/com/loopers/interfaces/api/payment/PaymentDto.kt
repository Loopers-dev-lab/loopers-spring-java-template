package com.loopers.interfaces.api.payment

import com.loopers.application.payment.OrderInfo
import com.loopers.application.payment.PaymentCommand
import com.loopers.application.payment.TransactionInfo
import com.loopers.domain.payment.CardType
import com.loopers.domain.payment.TransactionStatus
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

object PaymentDto {
    data class PaymentRequest(
        val orderId: String,
        val cardType: CardTypeDto,
        val cardNo: String,
        val amount: Long,
        val callbackUrl: String,
    ) {
        companion object {
            private val REGEX_CARD_NO = Regex("^\\d{4}-\\d{4}-\\d{4}-\\d{4}$")
            private const val PREFIX_CALLBACK_URL = "http://localhost:8080"
        }

        fun validate() {
            if (orderId.isBlank() || orderId.length < 6) {
                throw CoreException(ErrorType.BAD_REQUEST, "주문 ID는 6자리 이상 문자열이어야 합니다.")
            }
            if (!REGEX_CARD_NO.matches(cardNo)) {
                throw CoreException(ErrorType.BAD_REQUEST, "카드 번호는 xxxx-xxxx-xxxx-xxxx 형식이어야 합니다.")
            }
            if (amount <= 0) {
                throw CoreException(ErrorType.BAD_REQUEST, "결제금액은 양의 정수여야 합니다.")
            }
            if (!callbackUrl.startsWith(PREFIX_CALLBACK_URL)) {
                throw CoreException(ErrorType.BAD_REQUEST, "콜백 URL 은 $PREFIX_CALLBACK_URL 로 시작해야 합니다.")
            }
        }

        fun toCommand(userId: String): PaymentCommand.CreateTransaction =
            PaymentCommand.CreateTransaction(
                userId = userId,
                orderId = orderId,
                cardType = cardType.toCardType(),
                cardNo = cardNo,
                amount = amount,
                callbackUrl = callbackUrl,
            )
    }

    data class TransactionDetailResponse(
        val transactionKey: String,
        val orderId: String,
        val cardType: CardTypeDto,
        val cardNo: String,
        val amount: Long,
        val status: TransactionStatusResponse,
        val reason: String?,
    ) {
        companion object {
            fun from(transactionInfo: TransactionInfo): TransactionDetailResponse =
                TransactionDetailResponse(
                    transactionKey = transactionInfo.transactionKey,
                    orderId = transactionInfo.orderId,
                    cardType = CardTypeDto.from(transactionInfo.cardType),
                    cardNo = transactionInfo.cardNo,
                    amount = transactionInfo.amount,
                    status = TransactionStatusResponse.from(transactionInfo.status),
                    reason = transactionInfo.reason,
                )
        }
    }

    data class TransactionResponse(
        val transactionKey: String,
        val status: TransactionStatusResponse,
        val reason: String?,
    ) {
        companion object {
            fun from(transactionInfo: TransactionInfo): TransactionResponse =
                TransactionResponse(
                    transactionKey = transactionInfo.transactionKey,
                    status = TransactionStatusResponse.from(transactionInfo.status),
                    reason = transactionInfo.reason,
                )
        }
    }

    data class OrderResponse(
        val orderId: String,
        val transactions: List<TransactionResponse>,
    ) {
        companion object {
            fun from(orderInfo: OrderInfo): OrderResponse =
                OrderResponse(
                    orderId = orderInfo.orderId,
                    transactions = orderInfo.transactions.map { TransactionResponse.from(it) },
                )
        }
    }

    enum class CardTypeDto {
        SAMSUNG,
        KB,
        HYUNDAI,
        ;

        fun toCardType(): CardType = when (this) {
            SAMSUNG -> CardType.SAMSUNG
            KB -> CardType.KB
            HYUNDAI -> CardType.HYUNDAI
        }

        companion object {
            fun from(cardType: CardType) = when (cardType) {
                CardType.SAMSUNG -> SAMSUNG
                CardType.KB -> KB
                CardType.HYUNDAI -> HYUNDAI
            }
        }
    }

    enum class TransactionStatusResponse {
        PENDING,
        SUCCESS,
        FAILED,
        ;

        companion object {
            fun from(transactionStatus: TransactionStatus) = when (transactionStatus) {
                TransactionStatus.PENDING -> PENDING
                TransactionStatus.SUCCESS -> SUCCESS
                TransactionStatus.FAILED -> FAILED
            }
        }
    }
}
