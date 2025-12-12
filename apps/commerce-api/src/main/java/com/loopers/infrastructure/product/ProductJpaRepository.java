package com.loopers.infrastructure.product;

import com.loopers.domain.product.ProductStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 상품 JPA Repository
 */
public interface ProductJpaRepository extends JpaRepository<ProductEntity, Long> {

    /**
     * 상품 ID로 조회 (비관적 락)
     * - 재고 차감 등 동시성 제어가 필요한 경우 사용
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ProductEntity p WHERE p.id = :productId")
    Optional<ProductEntity> findByIdWithLock(@Param("productId") Long productId);

    /**
     * 브랜드별 상품 조회
     */
    List<ProductEntity> findByBrandId(Long brandId);

    /**
     * 상품 상태로 조회
     */
    List<ProductEntity> findByStatus(ProductStatus status);

    /**
     * 브랜드별 상품 조회 (좋아요 순 정렬)
     */
    @Query("SELECT p FROM ProductEntity p WHERE p.brandId = :brandId ORDER BY p.likeCount DESC, p.id ASC")
    List<ProductEntity> findByBrandIdOrderByLikeCountDesc(@Param("brandId") Long brandId);

    /**
     * 모든 상품 조회 (좋아요 순 정렬)
     */
    @Query("SELECT p FROM ProductEntity p ORDER BY p.likeCount DESC, p.id ASC")
    List<ProductEntity> findAllOrderByLikeCountDesc();

    /**
     * 모든 상품 조회 (가격 오름차순)
     */
    @Query("SELECT p FROM ProductEntity p ORDER BY p.price ASC, p.id ASC")
    List<ProductEntity> findAllOrderByPriceAsc();


    /**
     * 모든 상품 조회 (최신순)
     */
    @Query("SELECT p FROM ProductEntity p ORDER BY p.createdAt DESC, p.id ASC")
    List<ProductEntity> findAllOrderByCreatedAtDesc();
}
