package com.loopers.domain.like;

import java.util.Optional;

public interface BrandLikeRepository {
    Optional<BrandLikeModel> findByUserIdAndBrandId(Long userId, Long brandId);
    
    BrandLikeModel save(BrandLikeModel brandLikeModel);
    
    void delete(BrandLikeModel brandLikeModel);
    
    boolean existsByUserIdAndBrandId(Long userId, Long brandId);
    
    void deleteAll();
    
    long count();
}