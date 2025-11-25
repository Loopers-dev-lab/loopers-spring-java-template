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
     * 비관적 락을 사용하여 상품을 조회합니다.
     * 동시성 제어를 위해 재고 차감 시 사용됩니다.
     *
     * @param id 상품 ID
     * @return 상품 엔티티
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ProductEntity p WHERE p.id = :id AND p.deletedAt IS NULL")
    Optional<ProductEntity> findByIdWithLock(@Param("id") Long id);

    /**
     * 좋아요 수를 원자적으로 증가시킵니다.
     * DB 레벨에서 UPDATE 쿼리로 처리하여 동시성 문제를 해결합니다.
     *
     * @param productId 상품 ID
     */
    @Query("UPDATE ProductEntity p SET p.likeCount = p.likeCount + 1 WHERE p.id = :productId")
    @Modifying
    void incrementLikeCount(@Param("productId") Long productId);

    /**
     * 좋아요 수를 원자적으로 감소시킵니다.
     * DB 레벨에서 UPDATE 쿼리로 처리하여 동시성 문제를 해결합니다.
     * 좋아요 수가 0보다 클 때만 감소시킵니다.
     *
     * @param productId 상품 ID
     */
    @Query("UPDATE ProductEntity p SET p.likeCount = CASE WHEN p.likeCount > 0 THEN p.likeCount - 1 ELSE 0 END WHERE p.id = :productId")
    @Modifying
    void decrementLikeCount(@Param("productId") Long productId);
}
