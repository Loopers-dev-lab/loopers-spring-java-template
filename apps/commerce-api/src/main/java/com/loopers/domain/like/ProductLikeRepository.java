package com.loopers.domain.like;

import java.util.Optional;

public interface ProductLikeRepository {
    boolean existsByUserIdAndProductId(Long userId, Long productId);
    Optional<ProductLikeModel> findByUserIdAndProductId(Long userId, Long productId);
    ProductLikeModel save(ProductLikeModel like);
    void delete(Long userPkId, Long productId);
    int insertIgnore(Long userPkId, Long productId);
    int countByProductId(Long productId);
}
