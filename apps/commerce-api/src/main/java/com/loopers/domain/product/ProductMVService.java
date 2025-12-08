package com.loopers.domain.product;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.application.product.BatchUpdateResult;
import com.loopers.domain.product.dto.ProductSearchFilter;
import com.loopers.infrastructure.cache.CacheStrategy;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 상품 Materialized View 서비스
 * <p>
 * MV 테이블 조회 및 배치 동기화를 담당하는 서비스입니다.
 * 상품, 브랜드, 좋아요 정보를 통합하여 조회 성능을 최적화합니다.
 *
 * @author hyunjikoh
 * @since 2025. 11. 27.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class ProductMVService {

    private final ProductMVRepository mvRepository;
    private final ProductCacheService productCacheService;
    private final AtomicReference<ZonedDateTime> lastBatchTime =
            new AtomicReference<>(ZonedDateTime.now().minusYears(1)); // 초기값

    /**
     * 상품 ID로 MV를 조회합니다.
     *
     * @param productId 상품 ID
     * @return 상품 MV (존재하지 않으면 Optional.empty())
     */
    public ProductMaterializedViewEntity getById(Long productId) {
        return mvRepository.findById(productId).
                orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND_PRODUCT,
                        String.format("상품을 찾을 수 없습니다. (ID: %d)", productId)
                ));
    }

    /**
     * 브랜드별 상품 MV를 페이징 조회합니다.
     *
     * @param brandId  브랜드 ID
     * @param pageable 페이징 정보
     * @return 페이징된 상품 MV 목록
     */
    public Page<ProductMaterializedViewEntity> findByBrandId(Long brandId, Pageable pageable) {
        return mvRepository.findByBrandId(brandId, pageable);
    }

    /**
     * @param productIds 상품 ID 목록
     * @param pageable   페이징 정보
     * @return 페이징된 상품 MV 목록
     */
    public Page<ProductMaterializedViewEntity> findByIdsAsPage(List<Long> productIds, Pageable pageable) {
        List<ProductMaterializedViewEntity> mvEntities = mvRepository.findByIdIn(productIds);
        return new PageImpl<>(mvEntities, pageable, mvEntities.size());
    }

    // ========== MV 엔티티 조회 (캐시 전략 포함) ==========

    /**
     * 캐시 전략에 따라 MV 엔티티 목록을 조회합니다.
     * <p>
     * 도메인 엔티티만 반환하며, DTO 변환은 Facade에서 수행합니다.
     *
     * @param filter   검색 조건
     * @param strategy 캐시 전략
     * @return MV 엔티티 페이지
     */
    @Transactional(readOnly = true)
    public Page<ProductMaterializedViewEntity> getMVEntitiesByStrategy(
            ProductSearchFilter filter,
            CacheStrategy strategy
    ) {
        return switch (strategy) {
            case HOT -> getProductsWithCache(filter, CacheStrategy.HOT);
            case WARM -> getProductsWithCache(filter, CacheStrategy.WARM);
            default -> findBySearchFilter(filter);
        };
    }

    public Page<ProductMaterializedViewEntity> findBySearchFilter(ProductSearchFilter filter) {
        return mvRepository.findBySearchFilter(filter);
    }

    /**
     * 캐시를 사용하여 MV 엔티티를 조회합니다.
     */
    private Page<ProductMaterializedViewEntity> getProductsWithCache(
            ProductSearchFilter filter,
            CacheStrategy strategy
    ) {
        Long brandId = filter.brandId();
        Pageable pageable = filter.pageable();

        // 1. 캐시에서 ID 리스트 조회
        Optional<List<Long>> cachedIds = productCacheService.getProductIdsFromCache(
                strategy, brandId, pageable
        );

        if (cachedIds.isPresent() && !cachedIds.get().isEmpty()) {
            log.debug("{} 캐시 히트 - brandId: {}, page: {}", strategy, brandId, pageable.getPageNumber());
            return findByIdsAsPage(cachedIds.get(), pageable);
        }

        log.debug("{} 캐시 미스 - brandId: {}, page: {}", strategy, brandId, pageable.getPageNumber());

        // 2. 통합된 searchFilter 기반 조회로 변경
        Page<ProductMaterializedViewEntity> mvProducts = mvRepository.findBySearchFilter(filter);

        // 3. ID 리스트 캐싱
        List<Long> productIds = mvProducts.getContent().stream()
                .map(ProductMaterializedViewEntity::getProductId)
                .toList();

        productCacheService.cacheProductIds(strategy, brandId, pageable, productIds);

        return mvProducts;
    }

    /**
     * MV 테이블을 원본 테이블과 동기화합니다.
     * 배치 스케줄러에서 주기적으로 호출됩니다.
     *
     * @return 배치 업데이트 결과
     */
    @Transactional
    public BatchUpdateResult syncMaterializedView() {
        long startTime = System.currentTimeMillis();
        int updatedCount = 0;
        int createdCount = 0;

        // 변경 추적용
        Set<Long> changedProductIds = new HashSet<>();
        Set<Long> affectedBrandIds = new HashSet<>();

        try {
            log.info("MV 배치 동기화 시작 - 마지막 배치 시간: {}", lastBatchTime);

            List<ProductMVSyncDto> changedProducts = mvRepository.findChangedProductsForSync(lastBatchTime.get());

            if (changedProducts.isEmpty()) {
                log.info("변경된 상품이 없습니다.");
                long duration = System.currentTimeMillis() - startTime;
                lastBatchTime.set(ZonedDateTime.now()); // 배치 시간 갱신
                return BatchUpdateResult.success(0, 0, duration, changedProductIds, affectedBrandIds);
            }

            log.info("변경 감지: {}건", changedProducts.size());

            // 2. 변경된 상품 ID 목록 추출
            List<Long> productIds = changedProducts.stream()
                    .map(ProductMVSyncDto::getProductId)
                    .collect(Collectors.toList());

            // 3. 기존 MV 조회 (변경된 상품만)
            Map<Long, ProductMaterializedViewEntity> existingMVMap = mvRepository.findByIdIn(productIds).stream()
                    .collect(Collectors.toMap(ProductMaterializedViewEntity::getProductId, mv -> mv));

            // 4. MV 생성 또는 업데이트
            List<ProductMaterializedViewEntity> toSave = new ArrayList<>();

            for (ProductMVSyncDto dto : changedProducts) {
                ProductMaterializedViewEntity existingMV = existingMVMap.get(dto.getProductId());

                if (existingMV == null) {
                    // 신규 생성
                    ProductMaterializedViewEntity newMV = ProductMaterializedViewEntity.fromDto(dto);
                    toSave.add(newMV);
                    createdCount++;

                    // 변경 추적
                    changedProductIds.add(dto.getProductId());
                    affectedBrandIds.add(dto.getBrandId());
                } else {
                    // 기존 MV와 비교하여 실제 변경이 있는 경우만 업데이트
                    boolean hasChanges = ProductMaterializedViewEntity.hasActualChangesFromDto(existingMV, dto);

                    if (hasChanges) {
                        syncMVFromDto(existingMV, dto);
                        toSave.add(existingMV);
                        updatedCount++;

                        // 변경 추적
                        changedProductIds.add(dto.getProductId());
                        affectedBrandIds.add(dto.getBrandId());
                    }
                }
            }

            // 5. 일괄 저장
            if (!toSave.isEmpty()) {
                mvRepository.saveAll(toSave);
            }

            // 6. 배치 시간 갱신
            lastBatchTime.set(ZonedDateTime.now());

            long duration = System.currentTimeMillis() - startTime;
            log.info("MV 배치 동기화 완료 - 생성: {}건, 갱신: {}건, 변경된 상품: {}개, 영향받은 브랜드: {}개, 소요: {}ms",
                    createdCount, updatedCount, changedProductIds.size(), affectedBrandIds.size(), duration);

            return BatchUpdateResult.success(
                    createdCount,
                    updatedCount,
                    duration,
                    changedProductIds,
                    affectedBrandIds
            );

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("MV 배치 동기화 실패 - 소요: {}ms", duration, e);
            return BatchUpdateResult.failure(e.getMessage(), duration);
        }
    }


    /**
     * DTO로부터 기존 MV를 동기화합니다.
     */
    private void syncMVFromDto(ProductMaterializedViewEntity mv, ProductMVSyncDto dto) {
        mv.syncFromDto(dto);
    }


    @Transactional
    public void deleteById(Long productId) {
        mvRepository.deleteByProductIdIn(List.of(productId));
    }
}
