package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.Ranking;
import com.loopers.domain.ranking.RankingType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Repository
@RequiredArgsConstructor
public class RedisRankingRepository {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 특정 날짜의 TOP N 랭킹 조회
     * @param rankingType 랭킹 타입 (LIKE, VIEW, ORDER)
     * @param date 조회 날짜
     * @param limit 조회할 개수
     * @return 랭킹 리스트 (점수 내림차순)
     */
    public List<Ranking> getTopRanking(RankingType rankingType, LocalDate date, int limit) {
        String key = buildRankingKey(rankingType, date);

        // ZREVRANGE: 점수 높은 순으로 조회
        ZSetOperations<String, Object> zSetOps = redisTemplate.opsForZSet();
        Set<ZSetOperations.TypedTuple<Object>> result = zSetOps.reverseRangeWithScores(key, 0, limit - 1);

        if (result == null || result.isEmpty()) {
            log.info("랭킹 데이터 없음 - key: {}", key);
            return List.of();
        }

        return convertToRankings(result);
    }

    /**
     * 페이지네이션 지원 랭킹 조회
     * @param rankingType 랭킹 타입
     * @param date 조회 날짜
     * @param offset 시작 위치 (0-based)
     * @param limit 조회할 개수
     * @return 랭킹 리스트
     */
    public List<Ranking> getRankingWithPaging(
            RankingType rankingType,
            LocalDate date,
            int offset,
            int limit
    ) {
        String key = buildRankingKey(rankingType, date);

        ZSetOperations<String, Object> zSetOps = redisTemplate.opsForZSet();
        Set<ZSetOperations.TypedTuple<Object>> result =
                zSetOps.reverseRangeWithScores(key, offset, offset + limit - 1);

        if (result == null || result.isEmpty()) {
            return List.of();
        }

        return convertToRankings(result, offset);
    }

    /**
     * 특정 상품의 랭킹 및 점수 조회
     * @param rankingType 랭킹 타입
     * @param date 조회 날짜
     * @param productId 상품 ID
     * @return 랭킹  (없으면 null)
     */
    public Ranking getProductRanking(RankingType rankingType, LocalDate date, Long productId) {
        String key = buildRankingKey(rankingType, date);

        ZSetOperations<String, Object> zSetOps = redisTemplate.opsForZSet();

        // 점수 조회
        Double score = zSetOps.score(key, productId.toString());
        if (score == null) {
            return null;
        }

        // 순위 조회 (ZREVRANK: 0-based, 점수 높은 순)
        Long rank = zSetOps.reverseRank(key, productId.toString());
        if (rank == null) {
            return null;
        }

        return Ranking.builder()
                .rank((int) (rank + 1))
                .productId(productId)
                .score(score)
                .build();
    }

    /**
     * 전체 랭킹 개수 조회
     */
    public long getRankingSize(RankingType rankingType, LocalDate date) {
        String key = buildRankingKey(rankingType, date);
        Long size = redisTemplate.opsForZSet().size(key);
        return size != null ? size : 0;
    }

    private String buildRankingKey(RankingType rankingType, LocalDate date) {
        return rankingType.getKeyPrefix() + ":" + date.format(DATE_FORMATTER);
    }

    private List<Ranking> convertToRankings(Set<ZSetOperations.TypedTuple<Object>> result) {
        return convertToRankings(result, 0);
    }

    private List<Ranking> convertToRankings(
            Set<ZSetOperations.TypedTuple<Object>> result,
            int offset
    ) {
        List<Ranking> entries = new ArrayList<>();
        int rank = offset + 1;  // 1-based rank

        for (ZSetOperations.TypedTuple<Object> tuple : result) {
            if (tuple.getValue() == null) {
                continue;
            }

            Long productId = Long.parseLong(tuple.getValue().toString());
            Double score = tuple.getScore();

            entries.add(Ranking.builder()
                    .rank(rank++)
                    .productId(productId)
                    .score(score)
                    .build());
        }

        return entries;
    }

}
