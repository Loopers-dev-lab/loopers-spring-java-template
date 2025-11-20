package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import com.loopers.interfaces.api.order.OrderV1Dto;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class OrderFacade {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    @Transactional
    public OrderInfo createOrder(String userId, List<OrderV1Dto.OrderRequest.OrderItemRequest> items) {
        // User 정보 조회
        User user = userRepository.findUserByUserIdWithLock(userId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "사용자 정보가 없습니다"));

        // Product 조회 및 Map 생성
        Map<Product, Integer> productQuantities = new HashMap<>();
        for (OrderV1Dto.OrderRequest.OrderItemRequest item : items) {
            Product product = productRepository.findByIdWithLock(item.productId())
                    .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품 정보가 없습니다"));
            productQuantities.put(product, item.quantity());
        }

        // Order 생성
        Order order = Order.createOrder(user, productQuantities);

        // 재고 차감
        productQuantities.forEach((product, quantity) ->
            product.decreaseStock(quantity)
        );

        // 포인트 차감
        user.usePoint(order.getTotalPrice());

        // Order 저장
        Order savedOrder = orderRepository.save(order);

        return OrderInfo.from(savedOrder);
    }
}
