package com.loopers.infrastructure.scheduler;

import java.time.LocalDate;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.loopers.cache.RankingRedisService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 랭킹 Score Carry-Over 스케줄러
 * <p>
 * 콜드 스타트 문제 해결을 위해 매일 23:50에 전날 점수의 일부를 다음 날로 이월
 *
 * @author hyunjikoh
 * @since 2025.12.25
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RankingCarryOverScheduler {

    private final RankingRedisService rankingRedisService;

    /**
     * Carry-Over 가중치 (10%)
     * - 너무 높으면: 어제 인기 상품이 계속 상위 유지 (신선도 ↓)
     * - 너무 낮으면: 콜드 스타트 해결 효과 미미
     */
    private static final double CARRY_OVER_WEIGHT = 0.1;

    /**
     * 매일 23:50에 실행
     * <p>
     * 오늘 점수의 10%를 내일 키에 미리 복사하여 자정 콜드 스타트 방지
     */
    @Scheduled(cron = "0 50 23 * * *")
    public void carryOverDailyRanking() {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        log.info("랭킹 Carry-Over 스케줄러 시작: {} → {} (weight={})",
                today, tomorrow, CARRY_OVER_WEIGHT);

        try {
            long carryOverCount = rankingRedisService.carryOverScores(today, tomorrow, CARRY_OVER_WEIGHT);

            if (carryOverCount > 0) {
                log.info("랭킹 Carry-Over 완료: {}개 상품 이월됨", carryOverCount);
            } else {
                log.warn("랭킹 Carry-Over: 이월할 데이터 없음 (오늘 랭킹이 비어있음)");
            }

        } catch (Exception e) {
            log.error("랭킹 Carry-Over 실패", e);
            // 스케줄러 실패 시에도 서비스는 계속 동작 (Fallback으로 대응)
        }
    }

    /**
     * 수동 Carry-Over 실행 (테스트/운영용)
     *
     * @param sourceDate 원본 날짜
     * @param targetDate 대상 날짜
     * @return 이월된 상품 수
     */
    public long manualCarryOver(LocalDate sourceDate, LocalDate targetDate) {
        log.info("수동 Carry-Over 실행: {} → {}", sourceDate, targetDate);
        return rankingRedisService.carryOverScores(sourceDate, targetDate, CARRY_OVER_WEIGHT);
    }
}
