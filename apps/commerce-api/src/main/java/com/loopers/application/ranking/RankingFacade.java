package com.loopers.application.ranking;

import com.loopers.domain.ranking.Period;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RankingFacade {
    private final DailyRankingService dailyRankingService;
    private final WeeklyRankingService weeklyRankingService;
    private final MonthlyRankingService monthlyRankingService;

    public RankingV1Dto.ProductRankingPageResponse getProductRanking(String date, Period period, int page, int size) {
        switch (period) {
            case DAILY -> {
                return dailyRankingService.getDailyRanking(date, period, page, size);
            }
            case WEEKLY -> {
                return weeklyRankingService.getWeeklyTop100(date, period, page, size);
            }
            case MONTHLY -> {
                return monthlyRankingService.getMonthlyTop100(date, period, page, size);
            }
            default -> throw new CoreException(ErrorType.BAD_REQUEST, "지원하지 않는 기간입니다.");
        }
    }

}
