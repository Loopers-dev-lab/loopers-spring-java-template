package com.loopers.application.ranking;

import com.loopers.domain.ranking.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingScheduler {

    private final RankingService rankingService;
    private final Clock clock;

    @Value("${ranking.carry-over.weight:0.1}")
    private double carryOverWeight;

    /**
     * 매일 23:50에 실행
     * - 오늘 점수의 일부(10%)를 내일 키에 미리 복사
     */
    @Scheduled(cron = "0 50 23 * * *")
    public void prepareNextDayRanking() {
        // 명시적으로 오늘/내일 기준 계산 (시간 오차 방지)
        LocalDate today = LocalDate.now(clock);
        LocalDate tomorrow = today.plusDays(1);

        log.info("다음날 랭킹 준비 시작: {} → {}, weight={}", today, tomorrow, carryOverWeight);

        try {
            rankingService.carryOverScores(today, tomorrow, carryOverWeight);
            log.info("다음날 랭킹 준비 완료: {} → {}", today, tomorrow);
        } catch (Exception e) {
            log.error("다음날 랭킹 준비 실패: {} → {}", today, tomorrow, e);
        }
    }
}
