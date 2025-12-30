package com.loopers.domain.ranking;

import java.time.Duration;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.zset.Aggregate;
import org.springframework.data.redis.connection.zset.Weights;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class RankingService {

    private static final String RANKING_KEY_PREFIX = "ranking:all:";
    private static final int TTL_SECONDS = 2 * 24 * 60 * 60;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RedisTemplate<String, String> redisTemplateMaster;

    public RankingService(@Qualifier("redisTemplateMaster") RedisTemplate<String, String> redisTemplateMaster) {
        this.redisTemplateMaster = redisTemplateMaster;
    }

    public void incrementScore(Long productId, RankingWeight weight) {
        LocalDate date = LocalDate.now();

        String key = getRankingKey(date);
        String member = String.valueOf(productId);

        ZSetOperations<String, String> zSetOps = redisTemplateMaster.opsForZSet();
        Double newScore = zSetOps.incrementScore(key, member, weight.getWeight());

        redisTemplateMaster.expire(key, Duration.ofSeconds(TTL_SECONDS));

        log.debug("랭킹 점수 증가: productId={}, weight={}, date={}, newScore={}",
                productId, weight.getDescription(), date, newScore);
    }

    public void carryOverScore(LocalDate fromDate, LocalDate toDate) {
        String fromKey = getRankingKey(fromDate);
        String toKey = getRankingKey(toDate);

        ZSetOperations<String, String> zSetOps = redisTemplateMaster.opsForZSet();

        double weight = RankingWeight.CARRY_OVER.getWeight();
        zSetOps.unionAndStore(fromKey, Collections.emptyList(), toKey,
                Aggregate.SUM, Weights.of(weight));

        redisTemplateMaster.expire(toKey, Duration.ofSeconds(TTL_SECONDS));

        log.info("랭킹 점수 carry-over 완료: fromDate={}, toDate={}", fromDate, toDate);
    }

    private String getRankingKey(LocalDate date) {
        return RANKING_KEY_PREFIX + date.format(DATE_FORMATTER);
    }
}

