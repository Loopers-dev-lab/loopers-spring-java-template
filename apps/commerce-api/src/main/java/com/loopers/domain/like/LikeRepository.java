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
     * 상품별 좋아요 수를 집계합니다.
     *
     * @param productIds 상품 ID 목록
     * @return 상품 ID를 키로, 좋아요 수를 값으로 하는 Map
     */
    Map<Long, Long> countByProductIds(List<Long> productIds);

    /**
     * 모든 상품의 좋아요 수를 집계합니다.
     * <p>
     * 비동기 집계 스케줄러에서 사용됩니다.
     * </p>
     *
     * @return 상품 ID를 키로, 좋아요 수를 값으로 하는 Map
     */
    Map<Long, Long> countAllByProductIds();
}

