package com.loopers.core.service.order.component;

import com.loopers.core.domain.order.OrderItem;
import com.loopers.core.domain.order.repository.OrderItemRepository;
import com.loopers.core.domain.product.Product;
import com.loopers.core.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class OrderLineAllocator {

    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional
    public BigDecimal allocate(OrderItem orderItem) {
        Product product = productRepository.getByIdWithLock(orderItem.getProductId());
        productRepository.save(product.decreaseStock(orderItem.getQuantity()));
        orderItemRepository.save(orderItem);

        return product.getTotalPrice(orderItem.getQuantity());
    }
}
