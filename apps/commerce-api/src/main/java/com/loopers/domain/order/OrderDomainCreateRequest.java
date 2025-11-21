package com.loopers.domain.order;

import java.math.BigDecimal;

/**
 * 주문 생성 도메인 요청 DTO
 *
 * @param userId      사용자 ID
 * @param totalAmount 주문 총액
 * @author hyunjikoh
 * @since 2025. 11. 14.
 */
public record OrderDomainCreateRequest(
        Long userId,
        BigDecimal totalAmount
) {
    /**
     * 레코드 생성자 - 유효성 검증
     */
    public OrderDomainCreateRequest {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다.");
        }

        if (totalAmount == null) {
            throw new IllegalArgumentException("주문 총액은 필수입니다.");
        }

        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("주문 총액은 0보다 커야 합니다.");
        }
    }
}
