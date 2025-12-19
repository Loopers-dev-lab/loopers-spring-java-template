package com.loopers.infrastructure.event.payloads;

import java.math.BigDecimal;

/**
 * 결제 완료 이벤트 페이로드 V1
 * 상품별 개별 이벤트로 발행되며 주문 컨텍스트 정보를 포함합니다.
 *
 * @author hyunjikoh
 * @since 2025. 12. 17.
 */
public record PaymentSuccessPayloadV1(
        Long orderId,           // 주문 내부 ID (PK)
        Long orderNumber,       // 주문 번호 (비즈니스 키)
        Long userId,            // 주문한 사용자 ID
        Long productId,         // 상품 ID (개별 이벤트)
        Integer quantity,       // 구매 수량 (개별 이벤트)
        BigDecimal unitPrice,   // 단가
        BigDecimal totalPrice   // 해당 상품의 총 금액
) {
}
