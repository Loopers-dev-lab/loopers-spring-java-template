package com.loopers.domain.like;

import java.util.Optional;

/**
 * packageName : com.loopers.domain.like
 * fileName     : LikeRepository
 * author      : byeonsungmun
 * date        : 2025. 11. 12.
 * description :
 * ===========================================
 * DATE         AUTHOR       NOTE
 * -------------------------------------------
 * 2025. 11. 12.     byeonsungmun       최초 생성
 */
public interface LikeRepository {

    Optional<Like> findByUserIdAndProductId(String userId, Long productId);

    void save(Like like);

    void delete(Like like);

    long countByProductId(Long productId);
}
