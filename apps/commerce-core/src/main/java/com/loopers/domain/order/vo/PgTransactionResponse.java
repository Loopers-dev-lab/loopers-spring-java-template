package com.loopers.domain.order.vo;

/**
 * PG 결제 응답을 나타내는 값 객체
 * 도메인 레이어에서 사용하기 위해 infrastructure 의존성을 제거
 */
public record PgTransactionResponse(
        String transactionKey,
        String status,
        String reason
) {
    public PgTransactionResponse {
        if (transactionKey == null) {
            transactionKey = "";
        }
        if (status == null) {
            status = "";
        }
        if (reason == null) {
            reason = "";
        }
    }
}

