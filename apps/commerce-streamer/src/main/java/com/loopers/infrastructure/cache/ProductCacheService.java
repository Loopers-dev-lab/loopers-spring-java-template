package com.loopers.infrastructure.cache;

import org.springframework.stereotype.Component;

import com.loopers.cache.BaseCacheService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 상품 캐시 관리 서비스 (Streamer용)
 * <p>
 * 메트릭 변화 시 관련 캐시를 무효화합니다.
 * 공통 모듈의 BaseCacheService를 위임하여 사용합니다.
 *
 * @author hyunjikoh
 * @since 2025. 12. 18.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductCacheService {

    private final BaseCacheService baseCacheService;

    /**
     * 특정 상품의 캐시를 무효화
     */
    public void evictProductCache(Long productId) {
        baseCacheService.evictProductCache(productId);
    }

    /**
     * 상품 목록 관련 캐시들을 무효화
     */
    public void evictProductListCaches() {
        baseCacheService.evictProductListCaches();
    }

    /**
     * 판매량 변화 시 호출 - 인기 상품 순위가 변경될 수 있음
     */
    public void onSalesCountChanged(Long productId) {
        baseCacheService.onSalesCountChanged(productId);
    }


    /**
     * 브랜드별 상품 목록 캐시 무효화
     */
    public void evictBrandProductListCache(Long brandId) {
        baseCacheService.evictBrandProductListCache(brandId);
    }

    /**
     * 상품 상세 캐시의 재고 정보만 업데이트
     */
    public void updateProductStock(Long productId, Integer newStock) {
        baseCacheService.updateProductStock(productId, newStock);
    }
}
