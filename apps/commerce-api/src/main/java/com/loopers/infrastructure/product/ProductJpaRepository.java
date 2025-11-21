package com.loopers.infrastructure.product;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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
}
