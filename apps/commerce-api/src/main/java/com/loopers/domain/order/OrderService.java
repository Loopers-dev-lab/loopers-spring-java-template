package com.loopers.domain.order;

import com.loopers.domain.product.Product;
import com.loopers.domain.user.User;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    @Transactional
    public Order save(Order order) {
        return orderRepository.save(order);
    }

    public Order createOrderWithItems(
            User user,
            List<OrderItemRequest> itemRequests,
            Map<Long, Product> productMap
    ) {
        Order order = Order.create(user);

        for (OrderItemRequest itemRequest : itemRequests) {
            Product product = productMap.get(itemRequest.productId());
            order.addOrderItem(product, itemRequest.quantity());
        }

        return order;
    }

    public void restoreStock(Order order, Map<Long, Product> productMap) {
        for (OrderItem orderItem : order.getOrderItems()) {
            Product product = productMap.get(orderItem.getProductId());
            if (product == null) {
                throw new CoreException(ErrorType.NOT_FOUND,
                        "재고 복구 실패: 상품을 찾을 수 없습니다 - " + orderItem.getProductId());
            }
            product.increaseStock(orderItem.getQuantity());
        }
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByUser(User user) {
        return orderRepository.findAllByUser(user.getId());
    }

    @Transactional(readOnly = true)
    public Order getOrderByIdAndUser(Long orderId, User user) {
        return orderRepository.findByIdAndUser(orderId, user.getId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public Order getOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));
    }

    public record OrderItemRequest(Long productId, Integer quantity) {
        public static OrderItemRequest of(Long productId, Integer quantity) {
            return new OrderItemRequest(productId, quantity);
        }
    }
}
