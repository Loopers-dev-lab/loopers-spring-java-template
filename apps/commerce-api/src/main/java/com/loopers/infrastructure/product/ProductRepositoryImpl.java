package com.loopers.infrastructure.product;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.loopers.domain.product.ProductEntity;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.dto.ProductSearchFilter;

import lombok.RequiredArgsConstructor;

/**
 * @author hyunjikoh
 * @since 2025. 11. 10.
 */
@Component
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {
    private final ProductJpaRepository productJpaRepository;
    private final ProductQueryRepository productQueryRepository;

    @Override
    public ProductEntity save(ProductEntity product) {
        return productJpaRepository.save(product);
    }

    @Override
    public Page<ProductEntity> getProducts(ProductSearchFilter searchFilter) {
        return productQueryRepository.getProducts(searchFilter);
    }

    @Override
    public Optional<ProductEntity> findById(Long id) {
        return productJpaRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public Optional<ProductEntity> findByIdWithLock(Long id) {
        return productJpaRepository.findByIdWithLock(id);
    }

    @Override
    public void incrementLikeCount(Long productId) {
        productJpaRepository.incrementLikeCount(productId);
    }

    @Override
    public void decrementLikeCount(Long productId) {
        productJpaRepository.decrementLikeCount(productId);
    }
}
