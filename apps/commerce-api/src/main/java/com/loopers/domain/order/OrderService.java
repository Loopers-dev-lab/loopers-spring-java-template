package com.loopers.domain.order;

import com.loopers.domain.common.vo.DiscountResult;
import com.loopers.domain.product.Product;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class OrderService {
    private final OrderRepository orderRepository;

    public Order save(Order order) {
        return orderRepository.save(order);
    }

    public Order createOrder(Map<Product, Integer> productQuantityMap, Long userId, DiscountResult discountResult) {
        List<OrderItem> orderItems = new ArrayList<>();
        productQuantityMap.forEach((product, quantity) -> orderItems.add(OrderItem.create(
                product.getId(),
                product.getName(),
                quantity,
                product.getPrice()
        )));
        Order order = Order.create(userId, orderItems, discountResult);

        return orderRepository.save(order);
    }

    public Order getOrderByIdAndUserId(Long orderId, Long userId) {
        return orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));
    }

    public Page<Order> getOrdersByUserId(Long userId, Pageable pageable) {
        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        return orderRepository.findByUserIdAndDeletedAtIsNull(userId, PageRequest.of(page, size, sort));
    }
}
