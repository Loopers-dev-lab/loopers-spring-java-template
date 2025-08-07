package com.loopers.domain.like.product;

import com.loopers.domain.product.ProductModel;
import org.springframework.stereotype.Service;

@Service
public class ProductLikeService {
    
    public ProductLikeModel addLike(ProductModel product, Long userId) {
        var newLike = ProductLikeModel.create(userId, product.getId());
        product.incrementLikeCount();
        return newLike;
    }
    
    public void removeLike(ProductModel product, ProductLikeModel existingLike) {
        product.decrementLikeCount();
    }
    
    public LikeToggleResult toggleLike(ProductModel product, Long userId, ProductLikeModel existingLike) {
        if (existingLike != null) {
            product.decrementLikeCount();
            return LikeToggleResult.removed(existingLike);
        } else {
            var newLike = ProductLikeModel.create(userId, product.getId());
            product.incrementLikeCount();
            return LikeToggleResult.added(newLike);
        }
    }
    
    public static class LikeToggleResult {
        private final ProductLikeModel like;
        private final boolean isAdded;
        
        private LikeToggleResult(ProductLikeModel like, boolean isAdded) {
            this.like = like;
            this.isAdded = isAdded;
        }
        
        public static LikeToggleResult added(ProductLikeModel like) {
            return new LikeToggleResult(like, true);
        }
        
        public static LikeToggleResult removed(ProductLikeModel like) {
            return new LikeToggleResult(like, false);
        }
        
        public ProductLikeModel getLike() {
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
