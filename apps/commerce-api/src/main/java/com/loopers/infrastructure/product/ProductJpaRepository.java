package com.loopers.infrastructure.product;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.loopers.domain.product.ProductEntity;

import jakarta.persistence.LockModeType;

/**
 * @author hyunjikoh
 * @since 2025. 11. 10.
 */
public interface ProductJpaRepository extends JpaRepository<ProductEntity, Long> {

    Optional<ProductEntity> findByIdAndDeletedAtIsNull(Long id);

    /**
     * Retrieve a product by its ID while acquiring a pessimistic write lock.
     *
     * @param id the product ID
     * @return an Optional containing the product entity if it exists and is not deleted, otherwise an empty Optional
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ProductEntity p WHERE p.id = :id AND p.deletedAt IS NULL")
    Optional<ProductEntity> findByIdWithLock(@Param("id") Long id);

    /**
     * Atomically increments the like count for the specified product.
     *
     * @param productId the ID of the product whose like count will be incremented
     */
    @Query("UPDATE ProductEntity p SET p.likeCount = p.likeCount + 1 WHERE p.id = :productId")
    @Modifying
    void incrementLikeCount(@Param("productId") Long productId);

    /**
     * Atomically decreases a product's like count, ensuring the value does not drop below zero.
     *
     * @param productId the ID of the product whose like count should be decremented
     */
    @Query("UPDATE ProductEntity p SET p.likeCount = CASE WHEN p.likeCount > 0 THEN p.likeCount - 1 ELSE 0 END WHERE p.id = :productId")
    @Modifying
    void decrementLikeCount(@Param("productId") Long productId);
}