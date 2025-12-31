package com.loopers.ranking.streamer;

import com.loopers.cache.CacheKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

@Repository
@RequiredArgsConstructor
public class RankingReadRepository {

    private final RedisTemplate<String, String> redisTemplate;
    private final CacheKeyService cacheKeyService;

    /**
     * Top-N 조회
     */
    public Set<ZSetOperations.TypedTuple<String>> findTopN(
            LocalDate date,
            int size
    ) {
        return redisTemplate.opsForZSet()
                .reverseRangeWithScores(
                cacheKeyService.rankingKey(date),
                0,
                size - 1
        );
    }

    /**
     * 랭킹 페이지 Top-N 조회
     */
    public List<RankingInfo> findPage(
            LocalDate date,
            int page,
            int size
    ) {
        long start = (long) (page - 1) * size;
        long end = start + size - 1;
        long offset = (page - 1) * size;

        Set<ZSetOperations.TypedTuple<String>> zset = redisTemplate.opsForZSet().reverseRangeWithScores(
                cacheKeyService.rankingKey(date),
                start,
                end
        );

        if (zset == null || zset.isEmpty()) {
            return List.of();
        }

        List<ZSetOperations.TypedTuple<String>> zlist = List.copyOf(zset);

        return IntStream.range(0, zset.size())
                .mapToObj(i -> {
                    ZSetOperations.TypedTuple<String> s = zlist.get(i);
                    return new RankingInfo(
                            Long.valueOf(s.getValue()),
                            s.getScore().longValue(),
                            offset + i + 1
                    );
                })
                .toList();
    }

    /**
     * 특정 상품 순위 조회
     */
    public Long findRank(
            LocalDate date,
            Long productId
    ) {
        return redisTemplate.opsForZSet()
                .reverseRank(
                cacheKeyService.rankingKey(date),
                productId.toString()
        );
    }

    public long findTotalCount(LocalDate rankingDate) {
        return redisTemplate.opsForZSet()
                .zCard(rankingDate.toString());
    }
}
