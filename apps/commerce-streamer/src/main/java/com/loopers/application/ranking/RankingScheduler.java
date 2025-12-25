package com.loopers.application.ranking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 랭킹 점수 이월 스케줄러
 * 매일 자정에 전날 랭킹 점수의 일부를 오늘 랭킹으로 이월하여 콜드 스타트 문제를 해결
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RankingScheduler {

    private final ProductRankingService productRankingService;

    /**
     * 매일 자정 00:00에 실행: 전날 점수를 오늘 랭킹에 이월
     */
    @Scheduled(cron = "${ranking.carry-over.schedule:0 0 0 * * *}")
    public void initializeDailyRanking() {
        log.info("Starting daily ranking initialization with score carry-over...");
        
        try {
            // 전날 점수의 일부를 오늘 랭킹에 복사
            productRankingService.carryOverPreviousDayScore();
            
            log.info("Daily ranking initialization completed successfully");
        } catch (Exception e) {
            log.error("Failed to initialize daily ranking", e);
            // 실패 시 알림 발송 등 추가 처리 가능
        }
    }
}



