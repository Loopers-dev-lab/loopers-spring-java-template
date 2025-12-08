package com.loopers.infrastructure.external.dto;

import com.loopers.domain.payment.Payment;

public class PaymentExternalDto {

    /**
     * 결제 정보 Request
     * */
    public record PaymentRequest(
            String orderId,
            String cardType,
            String cardNo,
            Long amount,
            String callbackUrl
    ) {
        public static PaymentRequest from(Payment payment, String callbackUrl) {
            return new PaymentRequest(
                    payment.getPaymentId(),
                    payment.getCardType().name(),
                    payment.getCardNo(),
                    payment.getAmount().getAmount().longValue(),
                    callbackUrl
            );
        }
    }

    /**
     * 결제 정보 Response (접수 확인 응답)
     *
     * - PG에 결제 요청을 보낸 후 즉시 받는 응답
     * - 실제 결제는 백그라운드에서 비동기로 처리됨
     * - 최종 결제 결과는 콜백(POST /api/v1/payments/callback)으로 수신
     */
    public record PaymentResponse(
            Meta meta,
            PaymentData data
    ) {
        /**
         * 결제 요청 성공 여부 확인
         */
        public boolean isSuccess() {
            return "SUCCESS".equals(meta.result());
        }

        /**
         * Transaction Key 조회 (성공 시에만 존재)
         */
        public String getTransactionKey() {
            return data != null ? data.transactionKey() : null;
        }

        /**
         * 결제 상태 조회 (성공 시에만 존재)
         */
        public String getStatus() {
            return data != null ? data.status() : null;
        }

        /**
         * 에러 메시지 조회 (실패 시에만 존재)
         */
        public String getErrorMessage() {
            return meta.message();
        }
    }

    /**
     * Response Meta 정보
     *
     * @param result "SUCCESS" 또는 "FAIL"
     * @param errorCode 에러 코드 (실패 시)
     * @param message 에러 메시지 (실패 시)
     */
    public record Meta(
            String result,
            String errorCode,
            String message
    ) {
    }

    /**
     * Payment Data (성공 시에만 존재)
     *
     * @param transactionKey PG사에서 발급한 거래 키
     * @param status 결제 상태 ("PENDING")
     */
    public record PaymentData(
            String transactionKey,
            String status
    ) {
    }
}
