package com.loopers.core.infra.database.redis.product.impl;

import com.loopers.core.domain.product.repository.ProductLikeCacheRepository;
import com.loopers.core.domain.product.vo.ProductId;
import com.loopers.core.domain.productlike.ProductLikeCache;
import com.loopers.core.domain.user.vo.UserId;
import com.loopers.core.infra.database.redis.product.ProductLikeRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@RequiredArgsConstructor
public class ProductLikeCacheRepositoryImpl implements ProductLikeCacheRepository {

    private static final String LIKE_PRODUCT_CACHE_KEY_PREFIX = "like:product:";
    private static final String UNLIKE_PRODUCT_CACHE_KEY_PREFIX = "unlike:product:";
    private static final String LAST_SYNC_TIME_KEY = "like:sync:timestamp";

    private final ProductLikeRedisRepository repository;
    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void saveLike(ProductLikeCache productLikeCache) {
        repository.saveLike(
                productLikeCache.productId().value(),
                productLikeCache.userId().value(),
                productLikeCache.timestamp()
        );
    }

    @Override
    public void deleteLike(ProductLikeCache productLikeCache) {
        repository.deleteLike(productLikeCache.productId().value(), productLikeCache.userId().value());
    }

    @Override
    public void saveUnlike(ProductLikeCache productLikeCache) {
        repository.saveUnlike(
                productLikeCache.productId().value(),
                productLikeCache.userId().value(),
                productLikeCache.timestamp()
        );
    }

    @Override
    public void deleteUnlike(ProductLikeCache productLikeCache) {
        repository.deleteUnlike(productLikeCache.productId().value(), productLikeCache.userId().value());
    }

    @Override
    public List<ProductLikeCache> getLikesSinceLastSync(long lastSyncedTime, long currentTime) {
        Set<String> keys = redisTemplate.keys(LIKE_PRODUCT_CACHE_KEY_PREFIX + "*");
        List<ProductLikeCache> likes = new ArrayList<>();

        for (String key : keys) {
            String productId = key.replace(LIKE_PRODUCT_CACHE_KEY_PREFIX, "");

            Set<ZSetOperations.TypedTuple<String>> rangeByScore =
                    redisTemplate.opsForZSet().rangeByScoreWithScores(key, lastSyncedTime, currentTime);

            if (Objects.nonNull(rangeByScore)) {
                for (ZSetOperations.TypedTuple<String> tuple : rangeByScore) {
                    String userId = tuple.getValue();
                    Double timestamp = tuple.getScore();

                    ProductLikeCache like = new ProductLikeCache(
                            new ProductId(productId),
                            new UserId(userId),
                            Objects.requireNonNull(timestamp).longValue()
                    );
                    likes.add(like);
                }
            }
        }

        return likes;
    }

    @Override
    public List<ProductLikeCache> getUnlikesSinceLastSync(long lastSyncedTime, long currentTime) {
        Set<String> keys = redisTemplate.keys(UNLIKE_PRODUCT_CACHE_KEY_PREFIX + "*");
        List<ProductLikeCache> unlikes = new ArrayList<>();

        for (String key : keys) {
            String productId = key.replace(UNLIKE_PRODUCT_CACHE_KEY_PREFIX, "");

            Set<ZSetOperations.TypedTuple<String>> rangeByScore =
                    redisTemplate.opsForZSet().rangeByScoreWithScores(key, lastSyncedTime, currentTime);

            if (Objects.nonNull(rangeByScore)) {
                for (ZSetOperations.TypedTuple<String> tuple : rangeByScore) {
                    String userId = tuple.getValue();
                    Double timestamp = tuple.getScore();

                    ProductLikeCache unlike = new ProductLikeCache(
                            new ProductId(productId),
                            new UserId(userId),
                            Objects.requireNonNull(timestamp).longValue()
                    );
                    unlikes.add(unlike);
                }
            }
        }

        return unlikes;
    }

    @Override
    public long getLastSyncTime() {
        String lastSyncTime = redisTemplate.opsForValue().get(LAST_SYNC_TIME_KEY);

        return Optional.ofNullable(lastSyncTime)
                .map(Long::parseLong)
                .orElse(0L);
    }

    @Override
    public void updateLastSyncTime(long syncTime) {
        redisTemplate.opsForValue().set(LAST_SYNC_TIME_KEY, String.valueOf(syncTime));
    }
}
