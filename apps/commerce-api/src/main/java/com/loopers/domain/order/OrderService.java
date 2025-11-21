package com.loopers.domain.order;

import com.loopers.domain.product.Product;
import com.loopers.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@RequiredArgsConstructor
@Component
public class OrderService {

    private final OrderRepository orderRepository;

    @Transactional
    public Order createOrder(User user, Map<Product, Integer> productQuantities) {
        // Order 생성 (검증 포함)
        Order order = Order.createOrder(user, productQuantities);

        // 재고 차감
        productQuantities.forEach((product, quantity) -> {
            product.decreaseStock(quantity);
        });

        // 포인트 차감
        user.usePoint(order.getTotalPrice());

        // Order 저장
        return orderRepository.save(order);
    }
}
