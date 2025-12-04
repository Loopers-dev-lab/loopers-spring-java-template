package com.loopers.infrastructure.like;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.loopers.domain.like.LikeEntity;

/**
 * @author hyunjikoh
 * @since 2025. 11. 11.
 */
public interface LikeJpaRepository extends JpaRepository<LikeEntity, Long> {

    Optional<LikeEntity> findByUserIdAndProductId(Long userId, Long productId);

    /**
     * 특정 상품의 활성 좋아요 수를 조회한다.
     */
    Long countByProductIdAndDeletedAtIsNull(Long productId);

    /**
     * 좋아요가 있는 모든 상품 ID를 중복 없이 조회한다.
     */
    @Query("SELECT DISTINCT l.productId FROM LikeEntity l WHERE l.deletedAt IS NULL")
    List<Long> findDistinctProductIds();
}
