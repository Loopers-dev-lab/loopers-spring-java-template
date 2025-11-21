package com.loopers.domain.order.dto;

import java.math.BigDecimal;

/**
 * 주문 항목 생성 도메인 요청 DTO
 *
 * @param orderId        주문 ID
 * @param productId      상품 ID
 * @param couponId       쿠폰 ID
 * @param quantity       수량
 * @param unitPrice      단가
 * @param discountAmount 할인 금액
 * @author hyunjikoh
 * @since 2025. 11. 14.
 */
public record OrderItemDomainCreateRequest(
        Long orderId,
        Long productId,
        Long couponId,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal discountAmount
) {
    /**
     * 레코드 생성자 - 유효성 검증
     */
    public OrderItemDomainCreateRequest {
        if (orderId == null) {
            throw new IllegalArgumentException("주문 ID는 필수입니다.");
        }

        if (productId == null) {
            throw new IllegalArgumentException("상품 ID는 필수입니다.");
        }

        if (quantity == null) {
            throw new IllegalArgumentException("주문 수량은 필수입니다.");
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException("주문 수량은 1 이상이어야 합니다.");
        }

        if (quantity > 999) {
            throw new IllegalArgumentException("주문 수량은 999개를 초과할 수 없습니다.");
        }

        if (unitPrice == null) {
            throw new IllegalArgumentException("단가는 필수입니다.");
        }

        if (unitPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("단가는 0보다 커야 합니다.");
        }

        // discountAmount는 null이면 0으로 처리
        if (discountAmount != null && discountAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("할인 금액은 0 이상이어야 합니다.");
        }
    }
}
