package com.loopers.infrastructure.product;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import com.loopers.domain.product.ProductMVRepository;
import com.loopers.domain.product.ProductMVSyncDto;
import com.loopers.domain.product.ProductMaterializedViewEntity;
import com.loopers.domain.product.dto.ProductSearchFilter;

import lombok.RequiredArgsConstructor;

/**
 * 상품 Materialized View 리포지토리 구현체
 * <p>
 * Domain 계층의 ProductMVRepository 인터페이스를 구현합니다.
 * JPA Repository와 QueryDSL을 활용하여 데이터 접근을 처리합니다.
 *
 * @author hyunjikoh
 * @since 2025. 11. 27.
 */
@Component
@RequiredArgsConstructor
public class ProductMVRepositoryImpl implements ProductMVRepository {

    private final ProductMVJpaRepository jpaRepository;
    private final ProductMVQueryRepository queryRepository;

    @Override
    public Optional<ProductMaterializedViewEntity> findByProductId(Long productId) {
        return jpaRepository.findByProductId(productId);
    }


    @Override
    public Page<ProductMaterializedViewEntity> findByBrandId(Long brandId, Pageable pageable) {
        return queryRepository.findByBrandId(brandId, pageable);
    }

    @Override
    public Page<ProductMaterializedViewEntity> findByNameContaining(String keyword, Pageable pageable) {
        return queryRepository.findByNameContaining(keyword, pageable);
    }

    @Override
    public List<ProductMaterializedViewEntity> findByIdIn(List<Long> productIds) {
        return jpaRepository.findByProductIdIn(productIds);
    }

    @Override
    public List<ProductMaterializedViewEntity> findByLastUpdatedAtBefore(ZonedDateTime time) {
        return jpaRepository.findByLastUpdatedAtBefore(time);
    }

    @Override
    public ProductMaterializedViewEntity save(ProductMaterializedViewEntity entity) {
        return jpaRepository.save(entity);
    }

    @Override
    public List<ProductMaterializedViewEntity> saveAll(List<ProductMaterializedViewEntity> entities) {
        return jpaRepository.saveAll(entities);
    }

    @Override
    public void deleteByProductIdIn(List<Long> productIds) {
        jpaRepository.deleteByProductIdIn(productIds);
    }

    @Override
    public void deleteByBrandId(Long brandId) {
        jpaRepository.deleteByBrandId(brandId);
    }

    @Override
    public boolean existsByProductId(Long productId) {
        return jpaRepository.existsByProductId(productId);
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    @Override
    public Page<ProductMaterializedViewEntity> findBySearchFilter(ProductSearchFilter searchFilter) {
        return queryRepository.findBySearchFilter(searchFilter);
    }

    @Override
    public List<ProductMVSyncDto> findChangedProductsForSync(ZonedDateTime lastBatchTime) {
        return queryRepository.findChangedProductsForSync(lastBatchTime);
    }
}
