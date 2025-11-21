package com.loopers.domain.order.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 주문 생성 도메인 요청 DTO
 *
 * @param userId              사용자 ID
 * @param originalTotalAmount 할인 전 총액
 * @param discountAmount      총 할인 금액
 * @param finalTotalAmount    할인 후 총액
 * @author hyunjikoh
 * @since 2025. 11. 14.
 */
public record OrderDomainCreateRequest(
        Long userId,
        BigDecimal originalTotalAmount,
        BigDecimal discountAmount,
        BigDecimal finalTotalAmount
) {
    /**
     * 레코드 생성자 - 유효성 검증
     */
    public OrderDomainCreateRequest {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다.");
        }

        if (originalTotalAmount == null) {
            throw new IllegalArgumentException("할인 전 총액은 필수입니다.");
        }

        if (originalTotalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("할인 전 총액은 0보다 커야 합니다.");
        }

        if (discountAmount == null) {
            throw new IllegalArgumentException("할인 금액은 필수입니다.");
        }

        if (discountAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("할인 금액은 0 이상이어야 합니다.");
        }

        if (finalTotalAmount == null) {
            throw new IllegalArgumentException("할인 후 총액은 필수입니다.");
        }

        if (finalTotalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("할인 후 총액은 0보다 커야 합니다.");
        }

        // 금액 정합성 검증
        BigDecimal calculatedFinalAmount = originalTotalAmount.subtract(discountAmount);
        if (calculatedFinalAmount.setScale(2, RoundingMode.HALF_UP)
                .compareTo(finalTotalAmount.setScale(2, RoundingMode.HALF_UP)) != 0) {
            throw new IllegalArgumentException(
                    String.format("금액 정합성 오류: 할인 전 총액(%s) - 할인 금액(%s) ≠ 할인 후 총액(%s)",
                            originalTotalAmount, discountAmount, finalTotalAmount)
            );
        }
    }
}
