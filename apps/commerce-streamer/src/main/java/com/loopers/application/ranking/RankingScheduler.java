package com.loopers.application.ranking;

import com.loopers.domain.ranking.RankingKey;
import com.loopers.domain.ranking.RankingScore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 랭킹 관리 스케줄러
 * - 콜드 스타트 문제 해결을 위한 Score Carry-Over
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RankingScheduler {

    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 매일 23:50에 내일 랭킹 키 미리 생성 (콜드 스타트 해결)
     * - 오늘 점수의 10%를 내일 키로 복사
     */
    @Scheduled(cron = "0 50 23 * * *")  // 매일 23:50 실행
    public void prepareNextDayRanking() {
        try {
            String todayKey = RankingKey.dailyToday();
            String tomorrowKey = RankingKey.dailyTomorrow();

            log.info("🔄 내일 랭킹 키 생성 시작 - today: {}, tomorrow: {}", todayKey, tomorrowKey);

            // ZUNIONSTORE를 사용해 오늘 점수의 10%를 내일 키로 복사
            // destination key, numkeys, key1, weights, aggregate
            Long result = redisTemplate.opsForZSet().unionAndStore(
                todayKey,                              // source key
                java.util.Collections.singleton(tomorrowKey),  // destination keys
                tomorrowKey                            // destination key
            );

            if (result != null && result > 0) {
                // 가중치 0.1 적용 (모든 점수를 10%로 조정)
                multiplyAllScores(tomorrowKey, RankingScore.carryOverWeight());

                log.info("✅ 내일 랭킹 키 생성 완료 - key: {}, count: {}, weight: {}",
                    tomorrowKey, result, RankingScore.carryOverWeight());
            } else {
                log.warn("⚠️ 오늘 랭킹 데이터가 없어 내일 키를 생성하지 않음 - todayKey: {}", todayKey);
            }

        } catch (Exception e) {
            log.error("❌ 내일 랭킹 키 생성 실패", e);
        }
    }

    /**
     * ZSET의 모든 점수에 가중치 적용
     * - 각 멤버의 점수를 가중치만큼 곱함
     */
    private void multiplyAllScores(String key, double weight) {
        try {
            // ZRANGE로 모든 멤버와 점수 조회
            var entries = redisTemplate.opsForZSet().rangeWithScores(key, 0, -1);

            if (entries == null || entries.isEmpty()) {
                return;
            }

            // Pipeline으로 일괄 업데이트
            redisTemplate.executePipelined((org.springframework.data.redis.core.RedisCallback<Object>) connection -> {
                for (var entry : entries) {
                    String member = entry.getValue();
                    Double score = entry.getScore();

                    if (member != null && score != null) {
                        // 기존 점수 제거 후 새 점수로 추가
                        double newScore = score * weight;
                        connection.zAdd(key.getBytes(), newScore, member.getBytes());
                    }
                }
                return null;
            });

            log.debug("🔢 점수 가중치 적용 완료 - key: {}, weight: {}, count: {}",
                key, weight, entries.size());

        } catch (Exception e) {
            log.error("❌ 점수 가중치 적용 실패 - key: {}", key, e);
        }
    }
}
