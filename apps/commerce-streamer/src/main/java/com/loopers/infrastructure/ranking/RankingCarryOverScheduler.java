package com.loopers.infrastructure.ranking;

import com.loopers.ranking.RankingService;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingCarryOverScheduler {

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final double CARRY_OVER_WEIGHT = 0.5d;

    private final RankingService rankingService;

    @Scheduled(cron = "0 50 23 * * *", zone = "Asia/Seoul")
    public void carryOverForTomorrow() {
        LocalDate tomorrow = LocalDate.now(ZONE_ID).plusDays(1);
        try {
            rankingService.carryOver(tomorrow, CARRY_OVER_WEIGHT);
            log.info("Ranking carry-over completed for date={} weight={}", tomorrow, CARRY_OVER_WEIGHT);
        } catch (Exception exception) {
            log.error("Ranking carry-over failed for date={}", tomorrow, exception);
        }
    }
}
