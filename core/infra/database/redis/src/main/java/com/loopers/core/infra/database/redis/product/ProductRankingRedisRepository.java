package com.loopers.core.infra.database.redis.product;

import org.springframework.data.redis.core.ZSetOperations.TypedTuple;

import java.time.LocalDate;
import java.util.Set;

public interface ProductRankingRedisRepository {

    void increaseDaily(String productId, LocalDate date, Double score);

    Set<TypedTuple<String>> getTopProductsWithScores(LocalDate date, long start, long stop);

    Long countRankings(LocalDate date);

    Double getRankScore(LocalDate date, String productId);

    Long getRankingPosition(LocalDate date, String productId);

    void carryOverWithWeights(String sourceKey, String destKey, Double weight);

}
