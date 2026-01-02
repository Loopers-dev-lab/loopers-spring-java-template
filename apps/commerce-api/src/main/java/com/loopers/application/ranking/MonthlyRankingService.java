package com.loopers.application.ranking;

import com.loopers.domain.ranking.MonthlyRankingMv;
import com.loopers.domain.ranking.MonthlyRankingRepository;
import com.loopers.domain.ranking.Period;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MonthlyRankingService {
    private static final DateTimeFormatter BASIC = DateTimeFormatter.BASIC_ISO_DATE;

    private final MonthlyRankingRepository monthlyRankingRepository;

    public RankingV1Dto.ProductRankingPageResponse getMonthlyTop100(String date, Period period, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.max(size, 1);

        LocalDate end = LocalDate.parse(date, BASIC);
        String endDate = end.format(BASIC);

        String startDate = end.minusDays(30).format(BASIC);

        System.out.println("startDate = " + startDate);

        long totalElements = monthlyRankingRepository.countMonthly(startDate, endDate);

        if (totalElements == 0) {
            return new RankingV1Dto.ProductRankingPageResponse(
                    date,
                    period,
                    startDate,
                    endDate,
                    safePage,
                    safeSize,
                    0,
                    0,
                    List.of()
            );
        }

        int totalPages = (int) Math.ceil((double) totalElements / safeSize);

        long offset = (long) (safePage - 1) * safeSize;
        if (offset >= totalElements) {
            return new RankingV1Dto.ProductRankingPageResponse(
                    date,
                    period,
                    startDate,
                    endDate,
                    safePage,
                    safeSize,
                    totalElements,
                    totalPages,
                    List.of()
            );
        }

        List<MonthlyRankingMv> rows = monthlyRankingRepository.getMonthlyTop100(endDate, startDate, safePage, safeSize);

        List<RankingV1Dto.ProductRankingResponse> items = new ArrayList<>(rows.size());
        long rank = offset + 1;

        for (MonthlyRankingMv row : rows) {
            items.add(new RankingV1Dto.ProductRankingResponse(
                    rank++,
                    row.getProductId(),
                    row.getScore().doubleValue()
            ));
        }

        return new RankingV1Dto.ProductRankingPageResponse(
                date,
                period,
                startDate,
                endDate,
                safePage,
                safeSize,
                totalElements,
                totalPages,
                items
        );
    }
}
