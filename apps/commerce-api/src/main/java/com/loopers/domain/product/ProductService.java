package com.loopers.domain.product;

import com.loopers.domain.product.event.ProductCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Optional<Product> saveProduct(Product product) {
        Optional<Product> saved = productRepository.save(product);
        saved.ifPresent(p -> {
            // 이벤트 발행: ProductView 생성을 위해
            eventPublisher.publishEvent(new ProductCreatedEvent(p.getId(), p.getBrandId()));
        });
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<Product> findById(Long productId) {
        return productRepository.findById(productId);
    }

    @Transactional(readOnly = true)
    public Page<ProductView> findProductViews(ProductCondition condition, Pageable pageable) {
        return productRepository.findProductViews(condition, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<ProductView> findProductViewById(Long productId) {
        return productRepository.findProductViewById(productId);
    }
}
