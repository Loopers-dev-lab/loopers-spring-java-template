package com.loopers.domain.product;

import com.loopers.domain.product.event.ProductEvents;
import com.loopers.domain.product.event.ProductEventPublisher;
import com.loopers.domain.product.view.ProductCondition;
import com.loopers.domain.product.view.ProductView;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final ProductEventPublisher productEventPublisher;

    @Transactional
    public Optional<Product> createProduct(Product product) {
        Optional<Product> saved = productRepository.save(product);
        saved.ifPresent(p -> {
            // 이벤트 발행: ProductView 생성을 위해
            productEventPublisher.publishProductCreated(new ProductEvents.Created(p.getId(), p.getBrandId()));
        });
        return saved;
    }

    @Transactional
    public Optional<Product> updateProduct(Product product) {
        Optional<Product> saved = productRepository.save(product);
        saved.ifPresent(p -> {
            // 이벤트 발행: ProductView 업데이트를 위해
            productEventPublisher.publishProductUpdated(new ProductEvents.Updated(p.getId(), p.getBrandId()));
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
            productEventPublisher.publishProductDeleted(new ProductEvents.Deleted(productId));
        });
    }

    @Transactional(readOnly = true)
    public Product findById(Long productId) {
        log.info("Product 조회 시도 - productId: {}", productId);
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isEmpty()) {
            log.error("Product를 찾을 수 없습니다 - productId: {}", productId);
            throw new CoreException(ErrorType.NOT_FOUND, "[productId = " + productId + "] Product를 찾을 수 없습니다.");
        }
        Product product = productOpt.get();
        log.info("Product 조회 성공 - productId: {}, productName: {}, deletedAt: {}", 
                product.getId(), product.getName(), product.getDeletedAt());
        return product;
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
