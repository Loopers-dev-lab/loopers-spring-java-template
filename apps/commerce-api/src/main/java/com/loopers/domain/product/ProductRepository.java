package com.loopers.domain.product;

import java.util.Optional;

import org.springframework.data.domain.Page;

import com.loopers.domain.product.dto.ProductSearchFilter;

/**
 * @author hyunjikoh
 * @since 2025. 11. 10.
 */
public interface ProductRepository {
    ProductEntity save(ProductEntity product);

    Page<ProductEntity> getProducts(ProductSearchFilter searchFilter);

    Optional<ProductEntity> findById(Long id);

    /**
     * 비관적 락을 사용하여 상품을 조회합니다.
     * 동시성 제어가 필요한 재고 차감 시 사용됩니다.
     *
     * @param id 상품 ID
     * @return 상품 엔티티
     */
    Optional<ProductEntity> findByIdWithLock(Long id);
}
