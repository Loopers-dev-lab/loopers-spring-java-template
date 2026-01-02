package com.loopers.infrastructure.scheduler;

import com.loopers.domain.ranking.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 * 랭킹 Score Carry-Over 스케줄러
 * <p>
 * 매일 23시 50분에 실행되어 전날 점수의 10%를 오늘 랭킹에 복사합니다.
 * 이를 통해 콜드 스타트 문제를 완화합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RankingCarryOverScheduler {

    private static final double CARRY_OVER_WEIGHT = 0.1; // 10%
    private static final long TTL_SECONDS = 2 * 24 * 60 * 60; // 2일

    private final RankingService rankingService;
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * Lua 스크립트: 전날 점수의 일부를 오늘 랭킹에 복사
     */
    private static final String CARRY_OVER_SCRIPT =
            "local yesterdayKey = KEYS[1]\n" +
                    "local todayKey = KEYS[2]\n" +
                    "local weight = tonumber(ARGV[1])\n" +
                    "local ttl = tonumber(ARGV[2])\n" +
                    "\n" +
                    "local members = redis.call('ZRANGE', yesterdayKey, 0, -1, 'WITHSCORES')\n" +
                    "local count = 0\n" +
                    "for i = 1, #members, 2 do\n" +
                    "    local productId = members[i]\n" +
                    "    local score = tonumber(members[i + 1]) * weight\n" +
                    "    redis.call('ZINCRBY', todayKey, score, productId)\n" +
                    "    count = count + 1\n" +
                    "end\n" +
                    "\n" +
                    "redis.call('EXPIRE', todayKey, ttl)\n" +
                    "return count";

    /**
     * 매일 23시 50분에 실행 (다음 날 랭킹 미리 생성)
     * 전날 점수의 10%를 오늘 랭킹에 복사
     */
    @Scheduled(cron = "0 50 23 * * *") // 매일 23:50
    public void carryOverRankingScore() {
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            LocalDate today = LocalDate.now();

            String yesterdayKey = rankingService.getRankingKey(
                    yesterday.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            );
            String todayKey = rankingService.getRankingKey(
                    today.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
            );

            // 전날 랭킹이 존재하는지 확인
            if (Boolean.FALSE.equals(redisTemplate.hasKey(yesterdayKey))) {
                log.info("Yesterday ranking key does not exist, skipping carry-over: {}", yesterdayKey);
                return;
            }

            // Lua 스크립트 실행
            DefaultRedisScript<Long> script = new DefaultRedisScript<>(CARRY_OVER_SCRIPT, Long.class);
            Long carriedOverCount = redisTemplate.execute(
                    script,
                    Arrays.asList(yesterdayKey, todayKey),
                    String.valueOf(CARRY_OVER_WEIGHT),
                    String.valueOf(TTL_SECONDS)
            );

            log.info("Carried over {} products from {} to {} with weight {}",
                    carriedOverCount, yesterdayKey, todayKey, CARRY_OVER_WEIGHT);

        } catch (Exception e) {
            log.error("Failed to carry over ranking scores", e);
        }
    }
}

