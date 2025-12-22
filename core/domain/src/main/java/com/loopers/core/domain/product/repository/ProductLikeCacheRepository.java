package com.loopers.core.domain.product.repository;

import com.loopers.core.domain.productlike.ProductLikeCache;

import java.util.List;

public interface ProductLikeCacheRepository {

    void saveLike(ProductLikeCache productLikeCache);

    void deleteLike(ProductLikeCache productLikeCache);

    void saveUnlike(ProductLikeCache productLikeCache);

    void deleteUnlike(ProductLikeCache productLikeCache);

    List<ProductLikeCache> getLikesSinceLastSync(long lastSyncedTime, long currentTime);

    List<ProductLikeCache> getUnlikesSinceLastSync(long lastSyncedTime, long currentTime);

    long getLastSyncTime();

    void updateLastSyncTime(long syncTime);

    
}
