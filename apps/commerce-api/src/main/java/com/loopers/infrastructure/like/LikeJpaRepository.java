package com.loopers.infrastructure.like;

import com.loopers.domain.like.Like;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Like 엔티티를 위한 Spring Data JPA 리포지토리.
 */
public interface LikeJpaRepository extends JpaRepository<Like, Long> {
    /**
     * 사용자 ID와 상품 ID로 좋아요를 조회합니다.
     *
     * @param userId 사용자 ID
     * @param productId 상품 ID
     * @return 조회된 좋아요를 담은 Optional
     */
    Optional<Like> findByUserIdAndProductId(Long userId, Long productId);

    /**
     * 사용자 ID로 좋아요 목록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 좋아요 목록
     */
    List<Like> findAllByUserId(Long userId);

    /**
     * Aggregates like counts for the specified products.
     *
     * @param productIds the product IDs to include in the aggregation
     * @return a list of Object[] where each element is [productId (Long), likeCount (Long)]
     */
    @Query("SELECT l.productId, COUNT(l) FROM Like l WHERE l.productId IN :productIds GROUP BY l.productId")
    List<Object[]> countByProductIds(@Param("productIds") List<Long> productIds);

    /**
     * Aggregates total like counts for every product.
     *
     * Used by the asynchronous aggregation scheduler.
     *
     * @return a list of two-element Object arrays where each element is
     *         [productId (Long), likeCount (Long)]
     */
    @Query("SELECT l.productId, COUNT(l) FROM Like l GROUP BY l.productId")
    List<Object[]> countAllByProductIds();

    /**
     * 상품별 좋아요 수를 Map으로 변환합니다.
     *
     * @param productIds 상품 ID 목록
     * @return 상품 ID를 키로, 좋아요 수를 값으로 하는 Map
     */
    default Map<Long, Long> countByProductIdsAsMap(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return Map.of();
        }
        return countByProductIds(productIds).stream()
            .collect(Collectors.toMap(
                row -> (Long) row[0],
                row -> ((Number) row[1]).longValue()
            ));
    }
}
