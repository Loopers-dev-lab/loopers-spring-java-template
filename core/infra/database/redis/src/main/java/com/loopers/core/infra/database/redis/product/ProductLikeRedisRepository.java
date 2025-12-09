package com.loopers.core.infra.database.redis.product;

public interface ProductLikeRedisRepository {

    void saveLike(String productId, String userId, Long timestamp);

    void deleteLike(String productId, String userId);

    void saveUnlike(String productId, String userId, Long timestamp);

    void deleteUnlike(String productId, String userId);
}
