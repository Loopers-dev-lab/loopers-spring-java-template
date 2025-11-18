package com.loopers.application.order;

import com.loopers.domain.order.OrderItemModel;

import java.util.List;

public class OrderCommand {
    /**
     * 주문 유즈케이스에 대한 입력 DTO 입니다.
     * 선조회/실행에서 동일한 DTO를 사용합니다.
     * @param userId
     * @param orderLineRequests
     */
    public record Order (
        String userId,
        List<OrderLine> orderLineRequests
    ) { }

    /**
     * 주문 유즈케이스에서 주문 항목에 대한 입력 DTO 입니다.
     * 선조회/실행에서 동일한 DTO를 사용합니다.
     * @param productId
     * @param quantity
     */
    public record OrderLine(
            Long productId,
            int quantity
    ) {
        public static OrderLine from(OrderItemModel item) {
            return new OrderLine(
                    item.getProduct().getId(),
                    item.getQuantity()
            );
        }
    }
}
