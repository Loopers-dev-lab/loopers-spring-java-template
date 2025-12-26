package com.loopers.application.ranking;

import java.time.LocalDate;
import java.util.List;

public record RankingResult(
        List<RankingItemResult> rankings,
        int page,
        int size,
        LocalDate date
) {
    public static RankingResult empty(LocalDate date, int page, int size) {
        return new RankingResult(List.of(), page, size, date);
    }

}
