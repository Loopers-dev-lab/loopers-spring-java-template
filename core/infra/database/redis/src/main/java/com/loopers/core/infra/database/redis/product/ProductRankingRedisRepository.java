package com.loopers.core.infra.database.redis.product;

import java.time.LocalDateTime;

public interface ProductRankingRedisRepository {

    void increaseDaily(String productId, LocalDateTime date, Double score);
}
