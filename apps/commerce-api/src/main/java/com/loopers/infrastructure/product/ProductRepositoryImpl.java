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

    /**
     * Retrieve a product by its ID while acquiring a database lock to prevent concurrent updates.
     *
     * @param id the product identifier
     * @return an Optional containing the product if found, or empty if no matching product exists
     */
    @Override
    public Optional<ProductEntity> findByIdWithLock(Long id) {
        return productJpaRepository.findByIdWithLock(id);
    }

    /**
     * Increase the stored like count for the specified product.
     *
     * @param productId the database identifier of the product whose like count will be incremented
     */
    @Override
    public void incrementLikeCount(Long productId) {
        productJpaRepository.incrementLikeCount(productId);
    }

    /**
     * Decreases the persisted product's like count by one.
     *
     * @param productId the ID of the product whose like count should be decremented
     */
    @Override
    public void decrementLikeCount(Long productId) {
        productJpaRepository.decrementLikeCount(productId);
    }
}