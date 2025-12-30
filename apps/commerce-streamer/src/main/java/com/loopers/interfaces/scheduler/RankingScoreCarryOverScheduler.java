package com.loopers.interfaces.scheduler;

import com.loopers.domain.ranking.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@RequiredArgsConstructor
@Component
@Slf4j
public class RankingScoreCarryOverScheduler {

    private final RankingService rankingService;

    @Scheduled(cron = "0 50 23 * * *", zone = "Asia/Seoul")
    public void carryOverRankingScore() {
        try {
            LocalDate today = LocalDate.now();
            LocalDate targetDate = today.plusDays(1);

            rankingService.carryOverScore(today, targetDate);
        } catch (Exception e) {
            log.error("랭킹 점수 carry-over 중 오류 발생", e);
        }
    }
}

