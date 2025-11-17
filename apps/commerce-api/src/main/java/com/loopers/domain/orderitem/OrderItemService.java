package com.loopers.domain.orderitem;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class OrderItemService {

    private final OrderItemRepository orderItemRepository;

    /**
     * 특정 주문의 주문 항목들을 조회합니다.
     *
     * @param orderId 주문 ID
     * @return 주문 항목 리스트
     */
    public List<OrderItem> getOrderItemsByOrder(Long orderId) {
        return orderItemRepository.getOrderItemsByOrder(orderId);
    }
}
