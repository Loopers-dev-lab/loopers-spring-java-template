package com.loopers.domain.like;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Like 엔티티에 대한 저장소 인터페이스.
 * <p>
 * 좋아요 정보의 영속성 계층과의 상호작용을 정의합니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
public interface LikeRepository {
    /**
     * 좋아요를 저장합니다.
     *
     * @param like 저장할 좋아요
     * @return 저장된 좋아요
     */
    Like save(Like like);

    /**
     * 사용자 ID와 상품 ID로 좋아요를 조회합니다.
     *
     * @param userId 사용자 ID
     * @param productId 상품 ID
     * @return 조회된 좋아요를 담은 Optional
     */
    Optional<Like> findByUserIdAndProductId(Long userId, Long productId);

    /**
     * 좋아요를 삭제합니다.
     *
     * @param like 삭제할 좋아요
     */
    void delete(Like like);

    /**
     * 사용자 ID로 좋아요한 상품 목록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 좋아요 목록
     */
    List<Like> findAllByUserId(Long userId);

    /**
 * Aggregates like counts for the specified products.
 *
 * @param productIds list of product IDs to aggregate counts for
 * @return a map from product ID to its like count
 */
    Map<Long, Long> countByProductIds(List<Long> productIds);

    /**
 * Aggregates like counts for all products.
 *
 * Used by the asynchronous aggregation scheduler.
 *
 * @return a map whose keys are product IDs and whose values are the corresponding like counts
 */
    Map<Long, Long> countAllByProductIds();
}
