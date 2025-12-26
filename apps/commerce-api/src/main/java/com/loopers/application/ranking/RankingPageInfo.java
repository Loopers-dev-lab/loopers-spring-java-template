package com.loopers.application.ranking;

import com.loopers.domain.ranking.RankingInfo;

import java.time.LocalDate;
import java.util.List;

public record RankingPageInfo(
        List<RankingInfo> rankings,
        LocalDate date,
        int page,
        int size,
        Long totalCount,
        int totalPages
) {
    public static RankingPageInfo of(
            List<RankingInfo> rankings,
            LocalDate date,
            int page,
            int size,
            Long totalCount
    ) {
        int totalPages = (int) Math.ceil((double) totalCount / size);
        return new RankingPageInfo(rankings, date, page, size, totalCount, totalPages);
    }

    public static RankingPageInfo empty(LocalDate date, int page, int size) {
        return new RankingPageInfo(List.of(), date, page, size, 0L, 0);
    }
}
