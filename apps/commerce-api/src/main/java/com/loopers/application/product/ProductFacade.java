package com.loopers.application.product;

import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.brand.BrandEntity;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.like.LikeService;
import com.loopers.domain.product.*;
import com.loopers.domain.product.dto.ProductSearchFilter;
import com.loopers.domain.tracking.UserBehaviorTracker;
import com.loopers.domain.user.UserService;
import com.loopers.infrastructure.cache.CacheStrategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 상품 관련 유스케이스를 조정하는 Application Facade
 * Application Layer의 역할:
 * 도메인 서비스 호출 및 조정
 * 도메인 엔티티를 DTO로 변환
 * 트랜잭션 경계 관리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductFacade {
    private final ProductService productService;
    private final ProductMVService mvService;
    private final ProductCacheService productCacheService;
    private final LikeService likeService;
    private final UserService userService;
    private final BrandService brandService;
    private final UserBehaviorTracker behaviorTracker;

    /**
     * 도메인 서비스에서 MV 엔티티를 조회하고, Facade에서 DTO로 변환합니다.
     *
     * @param productSearchFilter 검색 조건
     * @return 상품 목록
     */
    @Transactional(readOnly = true)
    public Page<ProductInfo> getProducts(ProductSearchFilter productSearchFilter) {
        // 1. 캐시 전략 결정
        CacheStrategy strategy = productCacheService.determineCacheStrategy(productSearchFilter);

        // 2. 도메인 서비스에서 MV 엔티티 조회
        Page<ProductMaterializedViewEntity> mvEntities =
                mvService.getMVEntitiesByStrategy(productSearchFilter, strategy);

        // 4. DTO 변환
        return mvEntities.map(ProductInfo::from);
    }

    /**
     * 도메인 서비스에서 엔티티를 조회하고, Facade에서 DTO로 변환합니다.
     *
     * @param productId 상품 ID
     * @param username  사용자명 (nullable)
     * @return 상품 상세 정보
     */
    @Transactional(readOnly = true)
    public ProductDetailInfo getProductDetail(Long productId, String username) {
        // 1. 사용자 정보 및 좋아요 상태 조회
        Long userId = null;
        Boolean isLiked = false;

        if (username != null) {
            userId = userService.getUserByUsername(username).getId();
            isLiked = likeService.findLike(userId, productId)
                    .map(like -> like.getDeletedAt() == null)
                    .orElse(false);
        }

        // 2. 캐시 조회
        Optional<ProductDetailInfo> cachedDetail = productCacheService.getProductDetailFromCache(productId);

        ProductDetailInfo result;

        if (cachedDetail.isPresent()) {
            log.debug("상품 상세 캐시 히트 - productId: {}", productId);
            result = ProductDetailInfo.fromWithSyncLike(cachedDetail.get(), isLiked);
        } else {
            log.debug("상품 상세 캐시 미스 - productId: {}", productId);

            // 3. MV 엔티티 조회
            ProductMaterializedViewEntity productMaterializedViewEntity = mvService.getById(productId);
            result = ProductDetailInfo.from(productMaterializedViewEntity, isLiked);

            // 4. 캐시 저장
            productCacheService.cacheProductDetail(productId, result);
        }

        // 5. 유저 행동 추적 (상품 조회) - 캐시 히트/미스와 관계없이 항상 이벤트 발행
        if (userId != null) {
            behaviorTracker.trackProductView(
                    userId,
                    productId,
                    null  // searchKeyword는 Controller에서 받아야 함
            );
        }

        return result;
    }

    /**
     * 상품을 삭제합니다.
     * <p>
     * 상품 삭제 후 MV 동기화 및 캐시 무효화를 수행합니다.
     *
     * @param productId 상품 ID
     */
    @Transactional
    public void deletedProduct(Long productId) {
        // 1. 상품 삭제
        ProductEntity product = productService.getActiveProductDetail(productId);
        product.delete();

        // 2. MV 동기화
        mvService.deleteById(productId);

        // 3. 캐시 무효화
        productCacheService.getProductDetailFromCache(productId)
                .ifPresent(detail -> productCacheService.evictProductDetail(productId));
    }

    /**
     * 브랜드 삭제합니다.
     * <p>
     * 브랜드 삭제 후 MV 동기화 및 캐시 무효화를 수행합니다.
     *
     * @param brandId 브랜드 ID
     */
    @Transactional
    public void deletedBrand(Long brandId) {
        // 1. 브랜드 삭제
        BrandEntity brand = brandService.getBrandById(brandId);
        brand.delete();

        // 2. MV 동기화
        mvService.deleteByBrandId(brand.getId());

        // 3. 캐시 무효화
        productCacheService.evictBrandCaches(Set.of(brandId));
    }
}
