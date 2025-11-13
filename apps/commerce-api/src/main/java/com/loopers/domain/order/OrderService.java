package com.loopers.domain.order;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    @Transactional
    public Order createOrder(String userId, List<OrderItem> orderItems, int totalAmount) {
        if (orderItems == null || orderItems.isEmpty()) {
            throw new CoreException(
                    ErrorType.BAD_REQUEST,
                    "하나 이상의 상품을 주문해야 합니다."
            );
        }

        // 주문 생성 (PENDING)
        Order order = Order.create(userId, orderItems, totalAmount);

        // 주문 확정 (CONFIRMED)
        order.confirm();

        return orderRepository.save(order);
    }


    public record OrderItemRequest(Long productId, Long quantity) {}
}
