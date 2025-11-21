package com.loopers.domain.order;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 주문 상태 열거형
 *
 * <p>주문의 생명주기를 나타내는 상태값입니다.</p>
 *
 * @author hyunjikoh
 * @since 2025. 11. 14.
 */
@Getter
@RequiredArgsConstructor
public enum OrderStatus {

    /**
     * 대기 - 주문이 생성되었으나 아직 확정되지 않은 상태
     */
    PENDING("대기", "주문이 생성되었으나 아직 확정되지 않은 상태"),

    /**
     * 확정 - 결제가 완료되어 주문이 확정된 상태
     */
    CONFIRMED("확정", "결제가 완료되어 주문이 확정된 상태"),

    /**
     * 취소 - 주문이 취소된 상태
     */
    CANCELLED("취소", "주문이 취소된 상태");

    private final String description;
    private final String detail;
}
