package com.loopers.domain.order.event;

import com.loopers.interfaces.api.order.OrderDto;

public class OrderEvents {
    
    public record Created(
        Long userId,
        Long orderId,
        OrderDto.CreateOrderRequest request
    ) {}
    
    /**
     * 주문 완료 이벤트
     * 데이터 플랫폼 전송용
     */
    public record Confirmed(
        Long orderId,
        Long userId,
        String orderStatus
    ) {}
}

