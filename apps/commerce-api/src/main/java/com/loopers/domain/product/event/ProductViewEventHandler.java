package com.loopers.domain.product.event;

import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.like.LikeRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.view.ProductView;
import com.loopers.domain.product.view.ProductViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
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

    private static final String PRODUCT_INFO_KEY_PREFIX = "product:info:";
    private static final String PRODUCT_STAT_KEY_PREFIX = "product:stat:";
    private static final String LIKE_COUNT_FIELD = "likeCount";

    private final ProductViewRepository productViewRepository;
    private final LikeRepository likeRepository;
    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final RedisTemplate<String, Object> productCacheRedisTemplate;

    /**
     * 좋아요 수 변경 이벤트 처리
     * Write-Through 전략: Redis 캐시가 존재할 때만 원자적으로 증가/감소, DB에도 반영
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleProductLikeCountEvent(ProductEventDto.LikeCount event) {
        Long productId = event.productId();
        long delta = event.delta();
        
        try {
            // Redis 캐시 존재 여부 먼저 확인 (Write-Through: 캐시 없으면 무시)
            String statKey = PRODUCT_STAT_KEY_PREFIX + productId;
            Object existingValue = productCacheRedisTemplate.opsForHash().get(statKey, LIKE_COUNT_FIELD);
            
            if (existingValue != null) {
                // 캐시가 존재하면 값을 가져와서 증가/감소 후 다시 저장 (String 직렬화 일관성 유지)
                long currentCount = Long.parseLong(existingValue.toString());
                long newCount = Math.max(0, currentCount + delta); // 음수 방지
                productCacheRedisTemplate.opsForHash().put(statKey, LIKE_COUNT_FIELD, String.valueOf(newCount));
                log.debug("Redis cache likeCount updated: productId={}, delta={}, newCount={}", productId, delta, newCount);
            } else {
                log.debug("Redis cache not found for likeCount: productId={}, skipping cache update", productId);
            }

            // DB에도 반영 (정합성 확보)
            long realCount = likeRepository.countByProductId(productId);
            productViewRepository.updateLikeCount(productId, realCount);
            
            log.debug("ProductView likeCount updated: productId={}, likeCount={}", productId, realCount);
        } catch (Exception e) {
            log.error("Failed to update ProductView likeCount: productId={}", productId, e);
        }
    }

    /**
     * 상품 생성 이벤트 처리
     * Write-Around 전략: 캐시에 직접 쓰지 않음, TTL 끝나고 다시 가져오면 그때 반영
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleProductCreatedEvent(ProductEventDto.Created event) {
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

            // ProductView 생성
            ProductView productView = ProductView.builder()
                    .id(product.getId())
                    .name(product.getName())
                    .price(product.getPrice())
                    .likeCount(0L)  // 새로 생성된 상품이므로 좋아요 수는 0
                    .brandId(brandId)
                    .brandName(brandName)
                    .status(product.getStatus())
                    .createdAt(product.getCreatedAt())
                    .build();

            productViewRepository.save(productView);
            
            // Write-Around: 캐시에 직접 쓰지 않음 (TTL 끝나고 다시 가져오면 그때 반영)
            log.debug("ProductView created (Write-Around): productId={}, brandId={}", productId, brandId);
        } catch (Exception e) {
            log.error("Failed to create ProductView: productId={}", productId, e);
        }
    }

    /**
     * 상품 수정 이벤트 처리
     * Evict 전략: 캐시가 있으면 삭제 (다음 조회 시 최신 데이터로 갱신)
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleProductUpdatedEvent(ProductEventDto.Updated event) {
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

            // ProductView 업데이트
            productViewRepository.update(
                    productId,
                    product.getName(),
                    product.getPrice(),
                    brandId,
                    brandName,
                    product.getStatus()
            );

            // Evict: 캐시가 있으면 삭제 (캐시 없으면 무시)
            String infoKey = PRODUCT_INFO_KEY_PREFIX + productId;
            Boolean deleted = productCacheRedisTemplate.delete(infoKey);
            if (Boolean.TRUE.equals(deleted)) {
                log.debug("Redis cache evicted: productId={}, key={}", productId, infoKey);
            }

            log.debug("ProductView updated: productId={}, brandId={}", productId, brandId);
        } catch (Exception e) {
            log.error("Failed to update ProductView: productId={}", productId, e);
        }
    }

    /**
     * 상품 삭제 이벤트 처리 (Soft Delete)
     * Evict 전략: 캐시 삭제
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleProductDeletedEvent(ProductEventDto.Deleted event) {
        Long productId = event.productId();

        try {
            productViewRepository.deleteById(productId);
            
            // Evict: 캐시 삭제
            String infoKey = PRODUCT_INFO_KEY_PREFIX + productId;
            String statKey = PRODUCT_STAT_KEY_PREFIX + productId;
            productCacheRedisTemplate.delete(infoKey);
            productCacheRedisTemplate.delete(statKey);
            
            log.debug("ProductView deleted and cache evicted: productId={}", productId);
        } catch (Exception e) {
            log.error("Failed to delete ProductView: productId={}", productId, e);
        }
    }
}

