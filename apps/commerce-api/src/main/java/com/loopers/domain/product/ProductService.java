package com.loopers.domain.product;

import com.loopers.domain.product.event.ProductEventDto;
import com.loopers.domain.product.view.ProductCondition;
import com.loopers.domain.product.view.ProductView;
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
    public Optional<Product> createProduct(Product product) {
        Optional<Product> saved = productRepository.save(product);
        saved.ifPresent(p -> {
            // 이벤트 발행: ProductView 생성을 위해
            eventPublisher.publishEvent(new ProductEventDto.Created(p.getId(), p.getBrandId()));
        });
        return saved;
    }

    @Transactional
    public Optional<Product> updateProduct(Product product) {
        Optional<Product> saved = productRepository.save(product);
        saved.ifPresent(p -> {
            // 이벤트 발행: ProductView 업데이트를 위해
            eventPublisher.publishEvent(new ProductEventDto.Updated(p.getId(), p.getBrandId()));
        });
        return saved;
    }

    @Transactional
    public void deleteProduct(Long productId) {
        Optional<Product> productOpt = productRepository.findById(productId);
        productOpt.ifPresent(product -> {
            // Soft Delete: BaseEntity의 delete() 메서드 사용
            product.delete();
            productRepository.save(product);
            
            // 이벤트 발행: ProductView 삭제 및 캐시 Evict를 위해
            eventPublisher.publishEvent(new ProductEventDto.Deleted(productId));
        });
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
