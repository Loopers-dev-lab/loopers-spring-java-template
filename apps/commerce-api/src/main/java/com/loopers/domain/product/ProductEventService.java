package com.loopers.domain.product;

import com.loopers.domain.product.ProductEvent.ProductViewed;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Component
public class ProductEventService {
    private final ProductOutboxEventRepository productOutboxEventRepository;

    @Transactional
    public ProductOutboxEvent saveOutboxEvent(final ProductOutboxEvent productOutboxEvent) {
        return productOutboxEventRepository.save(productOutboxEvent);
    }

    public void findByProductEvent(final ProductViewed event) {
        productOutboxEventRepository.findBy
    }
}
