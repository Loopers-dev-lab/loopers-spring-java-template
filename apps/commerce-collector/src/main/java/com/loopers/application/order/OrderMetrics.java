package com.loopers.application.order;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 주문 메트릭 집계 데이터
 * - orderCount: 주문 건수 (해당 상품이 포함된 주문의 개수)
 * - totalQuantity: 총 주문 수량 (해당 상품의 총 수량)
 */
@Getter
@AllArgsConstructor
public class OrderMetrics {
    private final int orderCount;
    private final int totalQuantity;

    public static OrderMetrics of(int orderCount, int totalQuantity) {
        return new OrderMetrics(orderCount, totalQuantity);
    }

    public OrderMetrics add(int quantity) {
        return new OrderMetrics(
            this.orderCount + 1,           // 주문 건수 +1
            this.totalQuantity + quantity  // 수량 증가
        );
    }
}