package com.loopers.domain.like.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ProductLikeRepository {
    Optional<ProductLikeModel> findByUserIdAndProductId(Long userId, Long productId);
    
    ProductLikeModel save(ProductLikeModel productLikeModel);
    
    void delete(ProductLikeModel productLikeModel);
    
    boolean existsByUserIdAndProductId(Long userId, Long productId);
    
    Page<ProductLikeModel> findByUserIdOrderByLikedAtDesc(Long userId, Pageable pageable);
    
    void deleteAll();
    
    long count();
}
