package com.loopers.infrastructure.product;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;

import com.loopers.domain.product.ProductMaterializedViewEntity;

/**
 * 상품 Materialized View JPA 리포지토리
 * <p>
 * Spring Data JPA를 활용한 기본 CRUD 및 쿼리 메서드를 제공합니다.
 *
 * @author hyunjikoh
 * @since 2025. 11. 27.
 */
public interface ProductMVJpaRepository extends JpaRepository<ProductMaterializedViewEntity, Long> {

    /**
     * 상품 ID로 MV를 조회합니다.
     *
     * @param productId 상품 ID
     * @return 상품 MV
     */
    Optional<ProductMaterializedViewEntity> findByProductId(Long productId);

    /**
     * 여러 상품 ID로 MV를 일괄 조회합니다.
     *
     * @param productIds 상품 ID 목록
     * @return 상품 MV 목록
     */
    List<ProductMaterializedViewEntity> findByProductIdIn(@Param("productIds") List<Long> productIds);

    /**
     * 지정된 시간 이전에 업데이트된 MV를 조회합니다.
     *
     * @param time 기준 시간
     * @return 업데이트가 필요한 MV 목록
     */
    List<ProductMaterializedViewEntity> findByLastUpdatedAtBefore(ZonedDateTime time);

    /**
     * 지정된 상품 ID 목록에 해당하는 MV를 삭제합니다.
     *
     * @param productIds 삭제할 상품 ID 목록
     */
    @Modifying
    void deleteByProductIdIn(List<Long> productIds);

    /**
     * 상품 ID로 MV 존재 여부를 확인합니다.
     *
     * @param productId 상품 ID
     * @return 존재 여부
     */
    boolean existsByProductId(Long productId);
}
