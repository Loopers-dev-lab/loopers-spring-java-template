package com.loopers.domain.order.event;

import com.loopers.interfaces.api.order.OrderDto;

import java.math.BigDecimal;

public class OrderEvents {
    
    public record Created(
        Long userId,
        Long orderId,
        BigDecimal totalPrice,  // 쿠폰 할인 계산을 위해 필요
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

