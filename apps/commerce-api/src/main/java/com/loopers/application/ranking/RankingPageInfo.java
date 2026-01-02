package com.loopers.application.ranking;

import com.loopers.domain.ranking.RankingInfo;
import com.loopers.domain.ranking.RankingPeriod;

import java.time.LocalDate;
import java.util.List;

public record RankingPageInfo(
        List<RankingInfo> rankings,
        LocalDate date,
        RankingPeriod period,
        int page,
        int size,
        Long totalCount,
        int totalPages
) {
    public static RankingPageInfo of(List<RankingInfo> rankings, LocalDate date, RankingPeriod period,
                                     int page, int size, Long totalCount) {
        int totalPages = (int) Math.ceil((double) totalCount / size);
        return new RankingPageInfo(rankings, date, period, page, size, totalCount, totalPages);
    }

    public static RankingPageInfo empty(LocalDate date, RankingPeriod period, int page, int size) {
        return new RankingPageInfo(List.of(), date, period, page, size, 0L, 0);
    }

    public static RankingPageInfo of(List<RankingInfo> rankings, LocalDate date,
                                     int page, int size, Long totalCount) {
        return of(rankings, date, RankingPeriod.DAILY, page, size, totalCount);
    }

    public static RankingPageInfo empty(LocalDate date, int page, int size) {
        return empty(date, RankingPeriod.DAILY, page, size);
    }
}
