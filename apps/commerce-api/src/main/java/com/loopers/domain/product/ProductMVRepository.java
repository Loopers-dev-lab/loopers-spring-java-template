package com.loopers.domain.product;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.loopers.domain.product.dto.ProductSearchFilter;

/**
 * 상품 Materialized View 리포지토리 인터페이스
 * <p>
 * MV 테이블에 대한 데이터 접근 계층을 정의합니다.
 * 실제 구현은 Infrastructure 계층에서 제공됩니다.
 *
 * @author hyunjikoh
 * @since 2025. 11. 27.
 */
public interface ProductMVRepository {

    /**
     * ID로 상품 MV를 조회합니다.
     *
     * @param productId 상품 ID
     * @return 상품 MV (존재하지 않으면 Optional.empty())
     */
    Optional<ProductMaterializedViewEntity> findById(Long productId);

    /**
     * 브랜드별 상품 MV를 페이징 조회합니다.
     *
     * @param brandId  브랜드 ID
     * @param pageable 페이징 정보
     * @return 페이징된 상품 MV 목록
     */
    Page<ProductMaterializedViewEntity> findByBrandId(Long brandId, Pageable pageable);

    /**
     * 상품명으로 검색하여 MV를 페이징 조회합니다.
     *
     * @param keyword  검색 키워드 (상품명에 포함된 문자열)
     * @param pageable 페이징 정보
     * @return 페이징된 상품 MV 목록
     */
    Page<ProductMaterializedViewEntity> findByNameContaining(String keyword, Pageable pageable);

    /**
     * 여러 상품 ID로 MV를 일괄 조회합니다.
     *
     * @param productIds 상품 ID 목록
     * @return 상품 MV 목록
     */
    List<ProductMaterializedViewEntity> findByIdIn(List<Long> productIds);

    /**
     * 지정된 시간 이전에 업데이트된 MV를 조회합니다.
     * 배치 업데이트 시 변경된 데이터를 찾기 위해 사용됩니다.
     *
     * @param time 기준 시간
     * @return 업데이트가 필요한 MV 목록
     */
    List<ProductMaterializedViewEntity> findByLastUpdatedAtBefore(ZonedDateTime time);

    /**
     * MV 엔티티를 저장합니다.
     *
     * @param entity 저장할 MV 엔티티
     * @return 저장된 MV 엔티티
     */
    ProductMaterializedViewEntity save(ProductMaterializedViewEntity entity);

    /**
     * MV 엔티티 목록을 일괄 저장합니다.
     * 배치 업데이트 시 사용됩니다.
     *
     * @param entities 저장할 MV 엔티티 목록
     * @return 저장된 MV 엔티티 목록
     */
    List<ProductMaterializedViewEntity> saveAll(List<ProductMaterializedViewEntity> entities);

    /**
     * 지정된 상품 ID 목록에 해당하는 MV를 삭제합니다.
     * 상품이 삭제되었을 때 MV도 함께 삭제하기 위해 사용됩니다.
     *
     * @param productIds 삭제할 상품 ID 목록
     */
    void deleteByProductIdIn(List<Long> productIds);

    /**
     * MV가 존재하는지 확인합니다.
     *
     * @param productId 상품 ID
     * @return 존재 여부
     */
    boolean existsByProductId(Long productId);

    /**
     * 전체 MV 개수를 조회합니다.
     *
     * @return MV 개수
     */
    long count();

    /**
     * 검색 필터를 기반으로 상품 MV를 페이징 조회합니다.
     *
     * @param searchFilter 검색 필터 (브랜드 ID, 상품명, 페이징 정보)
     * @return 페이징된 상품 MV 목록
     */
    Page<ProductMaterializedViewEntity> findBySearchFilter(ProductSearchFilter searchFilter);


    /**
     * 지정된 시간 이후에 변경된 상품 MV를 동기화용 DTO로 조회합니다.
     * 배치 동기화 작업에 사용됩니다.
     *
     * @param lastBatchTime 마지막 배치 처리 시간
     * @return 변경된 상품 MV DTO 목록
     */
    List<ProductMVSyncDto> findChangedProductsForSync(ZonedDateTime lastBatchTime);
}
