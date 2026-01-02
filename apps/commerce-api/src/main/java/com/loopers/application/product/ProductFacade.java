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

import com.loopers.application.ranking.MonthlyRankingService;
import com.loopers.application.ranking.WeeklyRankingService;
import com.loopers.cache.CacheStrategy;
import com.loopers.cache.RankingRedisService;
import com.loopers.cache.dto.CachePayloads.RankingItem;
import com.loopers.domain.brand.BrandEntity;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.like.LikeService;
import com.loopers.domain.product.*;
import com.loopers.domain.product.dto.ProductSearchFilter;
import com.loopers.domain.ranking.MonthlyRankEntity;
import com.loopers.domain.ranking.RankingPeriod;
import com.loopers.domain.ranking.WeeklyRankEntity;
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
    private final WeeklyRankingService weeklyRankingService;
    private final MonthlyRankingService monthlyRankingService;

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
     * <p>
     * 콜드 스타트 Fallback: 오늘 랭킹이 비어있으면 어제 랭킹 반환
     *
     * @param pageable 페이징 정보
     * @param date     조회 날짜 (null이면 오늘)
     * @return 랭킹 상품 목록
     */
    @Transactional(readOnly = true)
    public Page<ProductInfo> getRankingProducts(Pageable pageable, LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now();
        
        // 1. 랭킹 조회
        List<RankingItem> rankings = rankingRedisService.getRanking(
                targetDate,
                pageable.getPageNumber() + 1, 
                pageable.getPageSize()
        );

        // 2. 콜드 스타트 Fallback: 오늘 랭킹이 비어있으면 어제 랭킹 조회
        if (rankings.isEmpty() && date == null) {
            LocalDate yesterday = targetDate.minusDays(1);
            log.info("콜드 스타트 Fallback: 오늘({}) 랭킹 없음, 어제({}) 랭킹 조회", targetDate, yesterday);
            
            rankings = rankingRedisService.getRanking(
                    yesterday,
                    pageable.getPageNumber() + 1,
                    pageable.getPageSize()
            );
            
            if (!rankings.isEmpty()) {
                targetDate = yesterday; // totalCount 계산을 위해 날짜 변경
            }
        }

        if (rankings.isEmpty()) {
            log.debug("랭킹 데이터 없음: date={}", targetDate);
            return Page.empty(pageable);
        }

        // 3. 상품 ID 목록 추출
        List<Long> productIds = rankings.stream()
                .map(RankingItem::productId)
                .collect(Collectors.toList());

        // 4. 상품 정보 조회 (MV 사용)
        List<ProductMaterializedViewEntity> products = mvService.getByIds(productIds);

        // 5. 랭킹 순서대로 정렬
        List<ProductInfo> sortedProducts = productIds.stream()
                .map(productId -> products.stream()
                        .filter(p -> p.getProductId().equals(productId))
                        .findFirst()
                        .map(ProductInfo::from)
                        .orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 6. Page 객체 생성
        long totalCount = rankingRedisService.getRankingCount(targetDate);
        return new PageImpl<>(sortedProducts, pageable, totalCount);
    }

    /**
     * 기간별 랭킹 상품 목록 조회
     * 
     * @param period 랭킹 기간 (DAILY, WEEKLY, MONTHLY)
     * @param pageable 페이징 정보
     * @param date 조회 날짜 (DAILY용, null이면 오늘)
     * @param yearWeek 조회 주차 (WEEKLY용, 예: "2024-W52")
     * @param yearMonth 조회 월 (MONTHLY용, 예: "2024-12")
     * @return 랭킹 상품 목록
     */
    @Transactional(readOnly = true)
    public Page<ProductInfo> getRankingProductsByPeriod(
            RankingPeriod period, 
            Pageable pageable, 
            LocalDate date, 
            String yearWeek, 
            String yearMonth) {
        
        return switch (period) {
            case DAILY -> getRankingProducts(pageable, date);
            case WEEKLY -> getWeeklyRankingProducts(pageable, yearWeek);
            case MONTHLY -> getMonthlyRankingProducts(pageable, yearMonth);
        };
    }

    /**
     * 주간 랭킹 상품 목록 조회
     * 
     * @param pageable 페이징 정보
     * @param yearWeek 조회 주차 (예: "2024-W52")
     * @return 주간 랭킹 상품 목록
     */
    @Transactional(readOnly = true)
    public Page<ProductInfo> getWeeklyRankingProducts(Pageable pageable, String yearWeek) {
        if (yearWeek == null || yearWeek.trim().isEmpty()) {
            log.warn("주간 랭킹 조회 시 yearWeek 파라미터가 필요합니다");
            return Page.empty(pageable);
        }

        // 1. 주간 랭킹 조회
        Page<WeeklyRankEntity> weeklyRankings = weeklyRankingService.getWeeklyRanking(yearWeek, pageable);
        
        if (weeklyRankings.isEmpty()) {
            log.debug("주간 랭킹 데이터 없음: yearWeek={}", yearWeek);
            return Page.empty(pageable);
        }

        // 2. 상품 ID 목록 추출
        List<Long> productIds = weeklyRankings.getContent().stream()
                .map(WeeklyRankEntity::getProductId)
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
        return new PageImpl<>(sortedProducts, pageable, weeklyRankings.getTotalElements());
    }

    /**
     * 월간 랭킹 상품 목록 조회
     * 
     * @param pageable 페이징 정보
     * @param yearMonth 조회 월 (예: "2024-12")
     * @return 월간 랭킹 상품 목록
     */
    @Transactional(readOnly = true)
    public Page<ProductInfo> getMonthlyRankingProducts(Pageable pageable, String yearMonth) {
        if (yearMonth == null || yearMonth.trim().isEmpty()) {
            log.warn("월간 랭킹 조회 시 yearMonth 파라미터가 필요합니다");
            return Page.empty(pageable);
        }

        // 1. 월간 랭킹 조회
        Page<MonthlyRankEntity> monthlyRankings = monthlyRankingService.getMonthlyRanking(yearMonth, pageable);
        
        if (monthlyRankings.isEmpty()) {
            log.debug("월간 랭킹 데이터 없음: yearMonth={}", yearMonth);
            return Page.empty(pageable);
        }

        // 2. 상품 ID 목록 추출
        List<Long> productIds = monthlyRankings.getContent().stream()
                .map(MonthlyRankEntity::getProductId)
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
        return new PageImpl<>(sortedProducts, pageable, monthlyRankings.getTotalElements());
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
