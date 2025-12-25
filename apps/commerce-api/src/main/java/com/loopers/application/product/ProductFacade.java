package com.loopers.application.product;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.cache.CacheStrategy;
import com.loopers.cache.RankingRedisService;
import com.loopers.cache.dto.CachePayloads.RankingItem;
import com.loopers.domain.brand.BrandEntity;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.like.LikeService;
import com.loopers.domain.product.*;
import com.loopers.domain.product.dto.ProductSearchFilter;
import com.loopers.domain.tracking.UserBehaviorTracker;
import com.loopers.domain.user.UserService;

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
    private final RankingRedisService rankingRedisService;

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
     * @param username    사용자 ID (nullable)
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

        // 5. 랭킹 정보 결합 (오늘 날짜 기준 실시간 순위 조회)
        final RankingItem ranking = rankingRedisService.getProductRanking(LocalDate.now(), productId);
        result = ProductDetailInfo.fromWithRanking(result, ranking);

        // 6. 유저 행동 추적 (이벤트 발행)
        if (userId != null) {
            behaviorTracker.trackProductView(userId, productId, null);
        }

        return result;
    }

    /**
     * 랭킹 상품 목록 조회
     *
     * @param pageable 페이징 정보
     * @param date     조회 날짜 (null이면 오늘)
     * @return 랭킹 상품 목록
     */
    @Transactional(readOnly = true)
    public Page<ProductInfo> getRankingProducts(Pageable pageable, LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        
        // 1. 랭킹 조회 (Redis-specific pagination logic is now encapsulated in rankingRedisService)
        List<RankingItem> rankings = rankingRedisService.getRanking(
                targetDate,
                pageable.getPageNumber() + 1, 
                pageable.getPageSize()
        );

        if (rankings.isEmpty()) {
            log.debug("랭킹 데이터 없음: date={}", targetDate);
            return Page.empty(pageable);
        }

        // 2. 상품 ID 목록 추출
        List<Long> productIds = rankings.stream()
                .map(RankingItem::productId)
                .collect(Collectors.toList());

        // 3. 상품 정보 조회 (MV 사용)
        List<ProductMaterializedViewEntity> products = mvService.getByIds(productIds);

        // 4. 랭킹 순서대로 정렬
        List<ProductInfo> sortedProducts = productIds.stream()
                .map(productId -> products.stream()
                        .filter(p -> p.getProductId().equals(productId))
                        .findFirst()
                        .map(ProductInfo::from)
                        .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 5. Page 객체 생성
        long totalCount = rankingRedisService.getRankingCount(targetDate);
        return new PageImpl<>(sortedProducts, pageable, totalCount);
    }

    /**
     * 상품을 삭제합니다.
     * <p>
     * 상품 삭제 후 MV 동기화 및 캐시 무효화를 수행합니다.
     *
     * @param productId 상품 ID
     */
    @Transactional
    public void deleteProduct(Long productId) {
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
    public void deleteBrand(Long brandId) {
        // 1. 브랜드 삭제
        BrandEntity brand = brandService.getBrandById(brandId);
        brand.delete();

        // 2. MV 동기화
        mvService.deleteByBrandId(brand.getId());

        // 3. 캐시 무효화
        productCacheService.evictBrandCaches(Set.of(brandId));
    }
}
