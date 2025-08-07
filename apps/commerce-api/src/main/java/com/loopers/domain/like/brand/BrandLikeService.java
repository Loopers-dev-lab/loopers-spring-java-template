package com.loopers.domain.like.brand;

import com.loopers.domain.brand.BrandModel;
import org.springframework.stereotype.Service;

@Service
public class BrandLikeService {
    
    public BrandLikeModel addLike(BrandModel brand, Long userId) {
        var newLike = BrandLikeModel.create(userId, brand.getId());
        brand.incrementLikeCount();
        return newLike;
    }
    
    public void removeLike(BrandModel brand, BrandLikeModel existingLike) {
        brand.decrementLikeCount();
    }
    
    public LikeToggleResult toggleLike(BrandModel brand, Long userId, BrandLikeModel existingLike) {
        if (existingLike != null) {
            brand.decrementLikeCount();
            return LikeToggleResult.removed(existingLike);
        } else {
            var newLike = BrandLikeModel.create(userId, brand.getId());
            brand.incrementLikeCount();
            return LikeToggleResult.added(newLike);
        }
    }
    
    public static class LikeToggleResult {
        private final BrandLikeModel like;
        private final boolean isAdded;
        
        private LikeToggleResult(BrandLikeModel like, boolean isAdded) {
            this.like = like;
            this.isAdded = isAdded;
        }
        
        public static LikeToggleResult added(BrandLikeModel like) {
            return new LikeToggleResult(like, true);
        }
        
        public static LikeToggleResult removed(BrandLikeModel like) {
            return new LikeToggleResult(like, false);
        }
        
        public BrandLikeModel getLike() {
            return like;
        }
        
        public boolean isAdded() {
            return isAdded;
        }
        
        public boolean isRemoved() {
            return !isAdded;
        }
    }
}
