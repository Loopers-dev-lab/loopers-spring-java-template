package com.loopers.infrastructure.like;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 좋아요 JPA Repository
 */
public interface LikeJpaRepository extends JpaRepository<LikeEntity, LikeEntity.LikeId> {

    /**
     * 특정 사용자의 특정 상품 좋아요 조회
     */
    Optional<LikeEntity> findByUserIdAndProductId(String userId, Long productId);

    /**
     * 좋아요 존재 여부 확인
     */
    boolean existsByUserIdAndProductId(String userId, Long productId);

    /**
     * 좋아요 삭제 (취소)
     */
    void deleteByUserIdAndProductId(String userId, Long productId);

    /**
     * 특정 상품의 좋아요 수 집계
     */
    int countByProductId(Long productId);

    /**
     * 특정 사용자가 좋아요한 상품 ID 목록
     */
    @Query("SELECT l.productId FROM LikeEntity l WHERE l.userId = :userId")
    List<Long> findProductIdsByUserId(@Param("userId") String userId);
}
