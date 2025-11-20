package com.loopers.domain.like;

import java.util.List;
import java.util.Optional;

public interface LikeRepository {
    Optional<Like> findByUserIdAndProductId(String userId, Long productId);
    Like save(Like like);
    void delete(Like like);
    //상품별 좋아요 수 조회
    Long countByProductId(Long productId);
    //유저가 좋아요한 상품 조회
    List<Like> findAllByUserId(String userId);
}
