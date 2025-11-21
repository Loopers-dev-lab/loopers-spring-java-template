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
 * Retrieve a product by id using a pessimistic lock to prevent concurrent modifications.
 *
 * Intended for operations that require concurrency control (for example, inventory deduction).
 *
 * @param id the product identifier
 * @return an Optional containing the ProductEntity if found, empty otherwise
 */
    Optional<ProductEntity> findByIdWithLock(Long id);

    /**
 * Atomically increments the like count for the specified product.
 *
 * Performs the update at the datastore level to ensure correctness under concurrent access.
 *
 * @param productId the identifier of the product whose like count will be incremented
 */
    void incrementLikeCount(Long productId);

    /**
 * Atomically decrement the like count of the specified product.
 *
 * Performs a database-level atomic decrement of the product's like count to ensure correctness under concurrent updates.
 *
 * @param productId the identifier of the product whose like count will be decremented
 */
    void decrementLikeCount(Long productId);
}