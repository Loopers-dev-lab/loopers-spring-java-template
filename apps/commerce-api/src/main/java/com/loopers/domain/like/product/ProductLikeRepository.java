package com.loopers.domain.like.product;

import java.util.Optional;

public interface ProductLikeRepository {
    Optional<ProductLikeModel> findByUserIdAndProductId(Long userId, Long productId);
    
    ProductLikeModel save(ProductLikeModel productLikeModel);
    
    void delete(ProductLikeModel productLikeModel);
    
    boolean existsByUserIdAndProductId(Long userId, Long productId);
    
    void deleteAll();
    
    long count();
}
