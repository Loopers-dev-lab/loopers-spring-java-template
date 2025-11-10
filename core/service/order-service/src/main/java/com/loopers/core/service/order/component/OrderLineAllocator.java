package com.loopers.core.service.order.component;

import com.loopers.core.domain.order.OrderedProduct;
import com.loopers.core.domain.product.Product;
import com.loopers.core.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class OrderLineAllocator {

    private final ProductRepository productRepository;

    public BigDecimal allocate(OrderedProduct orderedProduct) {
        Product product = productRepository.getById(orderedProduct.getProductId());
        productRepository.save(product.decreaseStock(orderedProduct.getQuantity()));

        return product.getTotalPrice(orderedProduct.getQuantity());
    }
}
