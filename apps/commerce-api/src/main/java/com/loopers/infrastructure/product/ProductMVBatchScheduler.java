package com.loopers.infrastructure.product;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.loopers.application.product.BatchUpdateResult;
import com.loopers.domain.brand.BrandEntity;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.product.ProductCacheService;
import com.loopers.domain.product.ProductMaterializedViewEntity;
import com.loopers.domain.product.ProductMVService;
import com.loopers.domain.product.dto.ProductSearchFilter;
import com.loopers.infrastructure.cache.CacheStrategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ProductMaterializedView 배치 스케줄러
 *
 * MV 테이블 동기화 및 Hot 캐시 갱신을 통합 관리합니다.
 *
 *   2분마다: MV 테이블 동기화
 *   50분마다: Hot 캐시 갱신 (인기순 상품, 브랜드별 인기순)
 *
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductMVBatchScheduler {
    private final ProductMVService mvService;
    private final ProductCacheService cacheService;
    private final BrandService brandService;

    // 배치 갱신 설정
    private static final int TOP_PRODUCTS_COUNT = 100;
    private static final int CACHE_PAGES_PER_BRAND = 3;
    private static final int PAGE_SIZE = 20;

    private boolean isFirstExecution = true; // 초기 실행 여부 플래그

    /**
     * MV 테이블 동기화 배치 작업 (2분마다)
     */
    @Scheduled(fixedDelay = 120000)
    public void syncMaterializedView() {
        log.info("MV 배치 업데이트 시작");

        try {
            BatchUpdateResult result = mvService.syncMaterializedView();

            if (!result.isSuccess()) {
                log.error("MV 배치 업데이트 실패 - error: {}", result.getErrorMessage());
                return;
            }

            if (result.hasChanges()) {
                if (isFirstExecution) {
                    log.info("초기 실행 - 불필요한 작업 생략");
                    isFirstExecution = false;
                    return;
                }

                log.info("변경사항 감지 - 선택적 캐시 무효화");
                cacheService.evictCachesAfterMVSync(
                        result.getChangedProductIds(),
                        result.getAffectedBrandIds()
                );
            } else {
                log.info("변경사항 없음");
            }

            log.info("MV 배치 업데이트 완료 - 생성: {}건, 갱신: {}건, 소요: {}ms",
                    result.getCreatedCount(), result.getUpdatedCount(), result.getDurationMs());

        } catch (Exception e) {
            log.error("MV 배치 업데이트 중 오류", e);
        }
    }

    /**
     * Hot 캐시 갱신 배치 작업 (50분마다)
     *
     * 배치 갱신으로 캐시 스탬피드 방지
     * ProductMVRepository를 직접 사용하여 likeCount 정렬 보장
     */
    @Scheduled(fixedRate = 50 * 60 * 1000, initialDelay = 60 * 1000)
    public void refreshHotCache() {
        log.info("Hot 캐시 갱신 시작");

        long startTime = System.currentTimeMillis();

        try {
            refreshPopularProductDetails();
            refreshBrandPopularProductIds();

            long duration = System.currentTimeMillis() - startTime;
            log.info("Hot 캐시 갱신 완료 - {}ms", duration);

        } catch (Exception e) {
            log.error("Hot 캐시 갱신 실패", e);
        }
    }

    /**
     * 인기 상품 상세 정보 갱신 (likeCount 상위 100개)
     */
    private void refreshPopularProductDetails() {
        log.debug("인기 상품 상세 갱신");

        try {
            Pageable pageable = PageRequest.of(0, TOP_PRODUCTS_COUNT,
                    Sort.by(Sort.Direction.DESC, "likeCount"));

            Page<ProductMaterializedViewEntity> popularProducts = mvService.findBySearchFilter(new ProductSearchFilter( null, null , pageable));

            List<Long> productIds = popularProducts.getContent().stream()
                    .map(ProductMaterializedViewEntity::getProductId)
                    .collect(Collectors.toList());

            cacheService.cacheProductIds(CacheStrategy.HOT, null, pageable, productIds);

            log.info("인기 상품 상세 갱신 완료 - {}개", productIds.size());

        } catch (Exception e) {
            log.error("인기 상품 상세 갱신 실패", e);
        }
    }

    /**
     * 브랜드별 인기순 상품 ID 리스트 갱신 (첫 3페이지)
     */
    private void refreshBrandPopularProductIds() {
        log.debug("브랜드별 인기순 ID 리스트 갱신");

        try {
            List<BrandEntity> brands = brandService.getAllBrands();
            int totalRefreshed = 0;

            for (BrandEntity brand : brands) {
                try {
                    int refreshed = refreshBrandProductIds(brand.getId());
                    totalRefreshed += refreshed;
                } catch (Exception e) {
                    log.warn("브랜드 ID 리스트 갱신 실패 - brandId: {}", brand.getId());
                }
            }

            log.info("브랜드별 ID 리스트 갱신 완료 - {}개 브랜드, {}페이지",
                    brands.size(), totalRefreshed);

        } catch (Exception e) {
            log.error("브랜드별 ID 리스트 갱신 실패", e);
        }
    }

    /**
     * 특정 브랜드의 인기순 상품 ID 리스트 갱신
     */
    private int refreshBrandProductIds(Long brandId) {
        int refreshedPages = 0;
        Sort sort = Sort.by(Sort.Direction.DESC, "likeCount");

        for (int page = 0; page < CACHE_PAGES_PER_BRAND; page++) {
            try {
                Pageable pageable = PageRequest.of(page, PAGE_SIZE, sort);


                Page<ProductMaterializedViewEntity> products =
                        mvService.findByBrandId(brandId, pageable);

                if (products.isEmpty()) {
                    break;
                }

                List<Long> productIds = products.getContent().stream()
                        .map(ProductMaterializedViewEntity::getProductId)
                        .collect(Collectors.toList());

                cacheService.cacheProductIds(CacheStrategy.HOT, brandId, pageable, productIds);
                refreshedPages++;

                log.debug("브랜드 ID 리스트 갱신 - brandId: {}, page: {}", brandId, page);

            } catch (Exception e) {
                log.warn("브랜드 ID 리스트 갱신 실패 - brandId: {}, page: {}", brandId, page);
            }
        }

        return refreshedPages;
    }
}
