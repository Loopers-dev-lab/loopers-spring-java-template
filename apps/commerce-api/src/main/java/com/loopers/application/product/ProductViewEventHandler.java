package com.loopers.application.product;

import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.like.LikeRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductView;
import com.loopers.domain.product.ProductViewRepository;
import com.loopers.domain.product.event.ProductCreatedEvent;
import com.loopers.domain.product.event.ProductDeletedEvent;
import com.loopers.domain.product.event.ProductLikeCountEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@RequiredArgsConstructor
@Component
public class ProductViewEventHandler {

    private final ProductViewRepository productViewRepository;
    private final LikeRepository likeRepository;
    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;

    /**
     * 좋아요 수 변경 이벤트 처리
     * Like 테이블에서 COUNT(*)를 수행하여 정합성을 보장
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleProductLikeCountEvent(ProductLikeCountEvent event) {
        Long productId = event.productId();
        
        try {
            // 실제 Like 테이블에서 Count 조회 (정합성 확보)
            long realCount = likeRepository.countByProductId(productId);

            // ProductView 업데이트
            productViewRepository.updateLikeCount(productId, realCount);
            
            log.debug("ProductView likeCount updated: productId={}, likeCount={}", productId, realCount);
        } catch (Exception e) {
            log.error("Failed to update ProductView likeCount: productId={}", productId, e);
        }
    }

    /**
     * 상품 생성 이벤트 처리
     * Product + Brand 조회 후 ProductView 생성
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleProductCreatedEvent(ProductCreatedEvent event) {
        Long productId = event.productId();
        Long brandId = event.brandId();
        
        try {
            // Product 조회
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new IllegalStateException("Product not found: " + productId));

            // Brand 조회
            String brandName = null;
            if (brandId != null) {
                brandName = brandRepository.findById(brandId)
                        .map(brand -> brand.getName())
                        .orElse(null);
            }

            // LikeCount 조회
            long likeCount = likeRepository.countByProductId(productId);

            // ProductView 생성
            ProductView productView = ProductView.builder()
                    .id(product.getId())
                    .name(product.getName())
                    .price(product.getPrice())
                    .likeCount(likeCount)
                    .brandId(brandId)
                    .brandName(brandName)
                    .status(product.getStatus())
                    .createdAt(product.getCreatedAt())
                    .build();

            productViewRepository.save(productView);
            
            log.debug("ProductView created: productId={}, brandId={}", productId, brandId);
        } catch (Exception e) {
            log.error("Failed to create ProductView: productId={}", productId, e);
        }
    }

    /**
     * 상품 삭제 이벤트 처리 (Soft Delete)
     * ProductView에서 해당 상품 제거
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleProductDeletedEvent(ProductDeletedEvent event) {
        Long productId = event.productId();

        try {
            productViewRepository.deleteById(productId);
            log.debug("ProductView deleted: productId={}", productId);
        } catch (Exception e) {
            log.error("Failed to delete ProductView: productId={}", productId, e);
        }
    }
}

