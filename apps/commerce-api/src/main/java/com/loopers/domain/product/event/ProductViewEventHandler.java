package com.loopers.domain.product.event;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.like.event.LikeEvents;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.view.ProductView;
import com.loopers.domain.product.view.ProductViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * ProductView 관련 이벤트 핸들러
 * ProductView Read Model의 CRUD 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductViewEventHandler {

    private final ProductViewRepository productViewRepository;
    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;

    @Transactional
    public void handleCreated(ProductEvents.Created event) {
        log.info("ProductViewEventHandler: ProductEvents.Created 처리 - productId: {}", event.productId());

        // Stale event 체크: Product 엔티티가 존재하는 경우 체크
        productRepository.findById(event.productId()).ifPresent(product -> {
            if (product.getLastEventOccurredAt() != null && 
                !event.getOccurredAt().isAfter(product.getLastEventOccurredAt())) {
                log.info("Stale event detected, skipping - productId: {}, eventOccurredAt: {}, lastEventOccurredAt: {}", 
                        event.productId(), event.getOccurredAt(), product.getLastEventOccurredAt());
                return;
            }
        });

        // Brand 조회하여 brandName 가져오기
        String brandName = brandRepository.findById(event.brandId())
                .map(Brand::getName)
                .orElse(null);

        // ProductView 생성
        ProductView productView = ProductView.builder()
                .id(event.productId())
                .name(event.name())
                .price(event.price())
                .brandId(event.brandId())
                .brandName(brandName)
                .status(event.status())
                .likeCount(0L)
                .createdAt(ZonedDateTime.of(event.getOccurredAt(), ZoneId.systemDefault()))
                .build();

        productViewRepository.save(productView);
        
        // Product 엔티티의 lastEventOccurredAt 업데이트
        productRepository.findById(event.productId()).ifPresent(product -> {
            product.updateLastEventOccurredAt(event.getOccurredAt());
            productRepository.save(product);
        });
        
        log.debug("ProductView 생성 완료 - productId: {}", event.productId());
    }

    @Transactional
    public void handleUpdated(ProductEvents.Updated event) {
        log.info("ProductViewEventHandler: ProductEvents.Updated 처리 - productId: {}", event.productId());

        // Stale event 체크: 이미 더 최신 이벤트가 처리되었는지 확인
        Product product = productRepository.findById(event.productId()).orElse(null);
        if (product != null && product.getLastEventOccurredAt() != null && 
            !event.getOccurredAt().isAfter(product.getLastEventOccurredAt())) {
            log.info("Stale event detected, skipping - productId: {}, eventOccurredAt: {}, lastEventOccurredAt: {}", 
                    event.productId(), event.getOccurredAt(), product.getLastEventOccurredAt());
            return;
        }

        // Brand 조회하여 brandName 가져오기
        String brandName = brandRepository.findById(event.brandId())
                .map(Brand::getName)
                .orElse(null);

        // ProductView 업데이트
        productViewRepository.update(
                event.productId(),
                event.name(),
                event.price(),
                event.brandId(),
                brandName,
                event.status()
        );
        
        // Product 엔티티의 lastEventOccurredAt 업데이트
        if (product != null) {
            product.updateLastEventOccurredAt(event.getOccurredAt());
            productRepository.save(product);
        }
        
        log.debug("ProductView 업데이트 완료 - productId: {}", event.productId());
    }

    @Transactional
    public void handleDeleted(ProductEvents.Deleted event) {
        log.info("ProductViewEventHandler: ProductEvents.Deleted 처리 - productId: {}", event.productId());

        // Stale event 체크: 이미 더 최신 이벤트가 처리되었는지 확인
        Product product = productRepository.findById(event.productId()).orElse(null);
        if (product != null && product.getLastEventOccurredAt() != null && 
            !event.getOccurredAt().isAfter(product.getLastEventOccurredAt())) {
            log.info("Stale event detected, skipping - productId: {}, eventOccurredAt: {}, lastEventOccurredAt: {}", 
                    event.productId(), event.getOccurredAt(), product.getLastEventOccurredAt());
            return;
        }

        productViewRepository.deleteById(event.productId());
        
        // Product 엔티티의 lastEventOccurredAt 업데이트
        if (product != null) {
            product.updateLastEventOccurredAt(event.getOccurredAt());
            productRepository.save(product);
        }
        
        log.debug("ProductView 삭제 완료 - productId: {}", event.productId());
    }

    @Transactional
    public void handleLikeCountChanged(LikeEvents.LikeCountChanged event) {
        updateLikeCount(event.productId(), event.delta());
    }

    @Transactional
    public void handleProductLikeSaved(LikeEvents.ProductLikeSaved event) {
        log.info("ProductViewEventHandler: ProductLikeSaved 처리 - productId: {}", event.productId());
        updateLikeCount(event.productId(), 1L);
    }

    @Transactional
    public void handleProductLikeDeleted(LikeEvents.ProductLikeDeleted event) {
        log.info("ProductViewEventHandler: ProductLikeDeleted 처리 - productId: {}", event.productId());
        updateLikeCount(event.productId(), -1L);
    }

    private void updateLikeCount(Long productId, long delta) {
        productViewRepository.findById(productId).ifPresent(view -> {
            long newLikeCount = view.getLikeCount() + delta;
            if (newLikeCount < 0) {
                newLikeCount = 0;
            }
            productViewRepository.updateLikeCount(productId, newLikeCount);
            log.debug("ProductView 좋아요 수 업데이트 완료 - productId: {}, newLikeCount: {}", 
                    productId, newLikeCount);
        });
    }
}

