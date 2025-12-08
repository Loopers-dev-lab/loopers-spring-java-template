package com.loopers.infrastructure.payment.client.dto;

import java.math.BigDecimal;

/**
 * PG API 응답 DTO
 * <p>
 * PG API 응답 구조:
 * {
 * "meta": {
 * "result": "SUCCESS" | "FAIL",
 * "errorCode": "...",  // FAIL 시
 * "message": "..."     // FAIL 시
 * },
 * "data": {
 * "transactionKey": "...",
 * "orderNumber": "...",
 * "cardType": "...",
 * "cardNo": "...",
 * "amount": 5000,
 * "status": "PENDING" | "SUCCESS" | "FAILED",
 * "reason": "..."
 * }
 * }
 *
 * @author hyunjikoh
 * @since 2025. 12. 2.
 */
public record PgPaymentResponse(
        Meta meta,
        Data data
) {
    /**
     * 메타 정보
     */
    public record Meta(
            String result,           // "SUCCESS" | "FAIL"
            String errorCode,        // FAIL 시
            String message           // FAIL 시
    ) {
        public boolean isSuccess() {
            return "SUCCESS".equals(result);
        }

        public boolean isFail() {
            return "FAIL".equals(result);
        }
    }

    /**
     * 결제 데이터
     */
    public record Data(
            String transactionKey,
            String orderId,
            String cardType,
            String cardNo,
            BigDecimal amount,
            String status,           // "PENDING" | "SUCCESS" | "FAILED"
            String reason            // 결과 사유
    ) {
        public boolean isPending() {
            return "PENDING".equals(status);
        }

        public boolean isSuccess() {
            return "SUCCESS".equals(status);
        }

        public boolean isFailed() {
            return "FAILED".equals(status);
        }
    }

    /**
     * API 호출 성공 여부 확인
     */
    public boolean isApiSuccess() {
        return meta != null && meta.isSuccess();
    }

    /**
     * API 호출 실패 여부 확인
     */
    public boolean isApiFail() {
        return meta != null && meta.isFail();
    }

    /**
     * 결제 상태 조회 (data가 null이 아닐 때)
     */
    public String getPaymentStatus() {
        return data != null ? data.status() : null;
    }

    /**
     * transactionKey 조회
     */
    public String transactionKey() {
        return data != null ? data.transactionKey() : null;
    }

    /**
     * orderNumber 조회
     */
    public String orderId() {
        return data != null ? data.orderId() : null;
    }

    /**
     * amount 조회
     */
    public BigDecimal amount() {
        return data != null ? data.amount() : null;
    }

    /**
     * status 조회 (backward compatibility)
     */
    public String status() {
        return getPaymentStatus();
    }

    /**
     * reason 조회
     */
    public String reason() {
        return data != null ? data.reason() : null;
    }
}

