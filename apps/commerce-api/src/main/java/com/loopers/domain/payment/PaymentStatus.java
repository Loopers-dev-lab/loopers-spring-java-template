package com.loopers.domain.payment;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author hyunjikoh
 * @since 2025. 12. 2.
 */
@Getter
@AllArgsConstructor
public enum PaymentStatus {
    PENDING("PG 요청 완료, 콜백 대기"),      // PG 요청 완료, 콜백 대기
    COMPLETED("결제 완료"),      // 결제 성공
    FAILED("결제 실패"),       // 결제 실패
    REFUNDED("결제 환불"),      // 결제 환불
    CANCEL("결제 취소"),      // 결제 취소
    TIMEOUT("결제 시간 만료");       // 콜백 미수신 타임아웃

    private final String description;

    public static PaymentStatus fromString(String status) {
        try {
            return valueOf(status);
        } catch (IllegalArgumentException e) {
            return FAILED; // 또는 적절한 기본값
        }
    }
}
