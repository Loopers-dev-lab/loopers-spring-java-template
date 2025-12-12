package com.loopers.application.payment;

import com.loopers.domain.payment.CardType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Payment 관련 Command 클래스들
 */
public class PaymentCommand {

    /**
     * 결제 요청 Command
     */
    @Getter
    @AllArgsConstructor
    public static class RequestPayment {
        private final String userId;
        private final Long orderId;
        private final CardType cardType;
        private final String cardNo;
        private final BigDecimal amount;
        private final String callbackUrl;

        public void validate() {
            if (userId == null || userId.trim().isEmpty()) {
                throw new IllegalArgumentException("사용자 ID는 필수입니다");
            }
            if (orderId == null || orderId <= 0) {
                throw new IllegalArgumentException("주문 ID는 양수여야 합니다");
            }
            if (cardType == null) {
                throw new IllegalArgumentException("카드 타입은 필수입니다");
            }
            if (cardNo == null || !cardNo.matches("^\\d{4}-\\d{4}-\\d{4}-\\d{4}$")) {
                throw new IllegalArgumentException("카드 번호는 xxxx-xxxx-xxxx-xxxx 형식이어야 합니다");
            }
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("결제 금액은 0보다 커야 합니다");
            }
            if (callbackUrl == null || callbackUrl.trim().isEmpty()) {
                throw new IllegalArgumentException("콜백 URL은 필수입니다");
            }
        }
    }

    /**
     * 결제 상태 업데이트 Command (콜백용)
     */
    @Getter
    @AllArgsConstructor
    public static class UpdatePaymentStatus {
        private final String pgTransactionKey;
        private final String status;  // PG 응답 상태
        private final String reason;
    }
}
