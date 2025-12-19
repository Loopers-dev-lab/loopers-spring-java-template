package com.loopers.application.payment;

import com.loopers.domain.payment.CardType;

public class PaymentCommand {

    public record RequestPayment(
            String orderId,
            CardType cardType,
            String cardNo,
            Long amount,
            String callbackUrl
    ) {
        public RequestPayment {
            if (orderId == null || orderId.isBlank()) {
                throw new IllegalArgumentException("주문 ID는 필수입니다.");
            }
            if (cardType == null) {
                throw new IllegalArgumentException("카드 타입은 필수입니다.");
            }
            if (cardNo == null || cardNo.isBlank()) {
                throw new IllegalArgumentException("카드 번호는 필수입니다.");
            }
            if (amount == null || amount < 0) {
                throw new IllegalArgumentException("결제 금액은 0 이상이어야 합니다.");
            }
            if (callbackUrl == null || callbackUrl.isBlank()) {
                throw new IllegalArgumentException("콜백 URL은 필수입니다.");
            }
        }
    }

    public record ProcessCallback(
            String transactionKey,
            String status,
            String reason
    ) {
        public ProcessCallback {
            if (transactionKey == null || transactionKey.isBlank()) {
                throw new IllegalArgumentException("트랜잭션 키는 필수입니다.");
            }
            if (status == null || status.isBlank()) {
                throw new IllegalArgumentException("상태는 필수입니다.");
            }
        }
    }
}
