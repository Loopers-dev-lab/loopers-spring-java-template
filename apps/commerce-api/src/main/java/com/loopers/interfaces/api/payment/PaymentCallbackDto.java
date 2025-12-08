package com.loopers.interfaces.api.payment;

public class PaymentCallbackDto {

    /**
     * PG에서 전송하는 콜백 요청
     *
     * @param transactionKey PG 거래 키 (예: "20251203:TR:1a5877")
     * @param orderId 주문 ID
     * @param cardType 카드사
     * @param cardNo 카드 번호
     * @param amount 결제 금액
     * @param status 결제 상태 ("SUCCESS", "FAILED")
     * @param reason 실패 사유 (실패 시에만 존재)
     */
    public record CallbackRequest(
            String transactionKey,
            String orderId,
            String cardType,
            String cardNo,
            Long amount,
            String status,
            String reason
    ) {
        /**
         * 결제 성공 여부 확인
         */
        public boolean isSuccess() {
            return "SUCCESS".equals(status);
        }

        /**
         * 결제 실패 여부 확인
         */
        public boolean isFailed() {
            return "FAILED".equals(status);
        }
    }
}
