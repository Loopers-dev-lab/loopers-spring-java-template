package com.loopers.infrastructure.product;

import com.loopers.domain.product.ProductOutboxEvent;
import com.loopers.domain.product.ProductOutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class ProductOutboxEventRepositoryImpl implements ProductOutboxEventRepository {
    private final ProductOutboxEventJpaRepository productOutboxEventJpaRepository;

    @Override
    public ProductOutboxEvent save(final ProductOutboxEvent productOutboxEvent) {
        return productOutboxEventJpaRepository.save(productOutboxEvent);
    }
}
