package com.loopers.infrastructure.cache;

import com.loopers.domain.ranking.RankingItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class RankingCacheService {

    private static final String RANKING_KEY_PREFIX = "ranking:all:";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RedisTemplate<String, String> redisTemplateMaster;

    public RankingCacheService(@Qualifier("redisTemplateMaster") RedisTemplate<String, String> redisTemplateMaster) {
        this.redisTemplateMaster = redisTemplateMaster;
    }

    public List<RankingItem> getRankingRange(LocalDate date, long start, long end) {
        String key = getRankingKey(date);

        ZSetOperations<String, String> zSetOps = redisTemplateMaster.opsForZSet();

        Set<ZSetOperations.TypedTuple<String>> entries = zSetOps.reverseRangeWithScores(key, start, end);

        if (entries == null || entries.isEmpty()) {
            log.debug("랭킹 데이터가 없음: key={}, start={}, end={}", key, start, end);
            return new ArrayList<>();
        }

        List<RankingItem> rankingItems = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> entry : entries) {
            String productIdStr = entry.getValue();
            Double score = entry.getScore();

            if (productIdStr == null || score == null) {
                continue;
            }

            try {
                Long productId = Long.parseLong(productIdStr);
                rankingItems.add(new RankingItem(productId, score));
            } catch (NumberFormatException e) {
                log.warn("랭킹에서 잘못된 productId 형식: {}", productIdStr);
            }
        }

        log.debug("랭킹 데이터 조회 완료: key={}, start={}, end={}, count={}", key, start, end, rankingItems.size());
        return rankingItems;
    }

    public Long getProductRank(LocalDate date, Long productId) {
        String key = getRankingKey(date);
        ZSetOperations<String, String> zSetOps = redisTemplateMaster.opsForZSet();

        String productIdStr = String.valueOf(productId);
        Long rank = zSetOps.reverseRank(key, productIdStr);

        if (rank == null) {
            log.debug("해당 상품이 랭킹에 존재하지 않음: key={}, productId={}", key, productId);
            return null;
        }

        return rank + 1;
    }

    private String getRankingKey(LocalDate date) {
        return RANKING_KEY_PREFIX + date.format(DATE_FORMATTER);
    }
}
