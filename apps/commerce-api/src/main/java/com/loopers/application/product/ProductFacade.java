package com.loopers.application.product;

import com.fasterxml.jackson.core.type.TypeReference;
import com.loopers.cache.CacheKeyService;
import com.loopers.cache.CacheService;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.ranking.RankingService;
import com.loopers.messaging.event.ProductViewedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;

@RequiredArgsConstructor
@Component
@Slf4j
public class ProductFacade {
    private final CacheKeyService cacheKeyService;
    private final CacheService cacheService;

    private final ProductService productService;
    private final ProductQueryService productQueryService;
    private final ApplicationEventPublisher eventPublisher;

    private final RankingService rankingService;

    private static final Duration TTL_LIST = Duration.ofMinutes(10);
    private static final Duration TTL_DETAIL = Duration.ofMinutes(5);


    public Page<ProductInfo> getProducts(Long brandId, Pageable pageable, String sort) {
        String key = cacheKeyService.productListKey(brandId, pageable, sort);

        return cacheService.getOrLoad(
                key,
                () -> loadProducts(brandId, pageable, sort),
                TTL_LIST,
                new TypeReference<Page<ProductInfo>>() {}
        );
    }

    private Page<ProductInfo> loadProducts(Long brandId, Pageable pageable, String sort) {
        if("likes_desc".equals(sort)) {
            return productQueryService.getProductsSortedByLikes(brandId, pageable);
        } else {
            return productQueryService.getProducts(brandId, pageable, sort);
        }
    }


    @Transactional(readOnly = true)
    public ProductDetailInfo getProductDetail(Long productId) {
        String key = "product:v1:detail:" + productId;

        Long rank = rankingService.getRanking(LocalDate.now(), productId);

        ProductDetailInfo productDetailInfo = cacheService.getOrLoad(
                key,
                () -> ProductDetailInfo.of(productService.getProductDetail(productId), rank),
                TTL_DETAIL,
                ProductDetailInfo.class);

        // 상품 상세 조회 시 ProductViewedEvent 발행
        eventPublisher.publishEvent(ProductViewedEvent.from(productId));

        return productDetailInfo;
    }
}
