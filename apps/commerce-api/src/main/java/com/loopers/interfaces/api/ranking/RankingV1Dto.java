package com.loopers.interfaces.api.ranking;

import com.loopers.application.ranking.RankingPageInfo;
import com.loopers.domain.ranking.RankingInfo;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class RankingV1Dto {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    public record RankingPageResponse(
            List<RankingItemResponse> rankings,
            String date,
            int page,
            int size,
            Long totalCount,
            int totalPages
    ) {
        public static RankingPageResponse from(RankingPageInfo info) {
            List<RankingItemResponse> items = info.rankings().stream()
                    .map(RankingItemResponse::from)
                    .toList();

            return new RankingPageResponse(
                    items,
                    info.date().format(DATE_FORMATTER),
                    info.page(),
                    info.size(),
                    info.totalCount(),
                    info.totalPages()
            );
        }
    }

    public record RankingItemResponse(
            Long productId,
            String productName,
            Long price,
            String brandName,
            Long rank,
            Double score
    ) {
        public static RankingItemResponse from(RankingInfo info) {
            return new RankingItemResponse(
                    info.productId(),
                    info.productName(),
                    info.price(),
                    info.brandName(),
                    info.rank(),
                    info.score()
            );
        }
    }

    public record TopNResponse(
            List<RankingItemResponse> rankings,
            String date,
            int size
    ) {
        public static TopNResponse of(List<RankingInfo> rankings, LocalDate date) {
            List<RankingItemResponse> items = rankings.stream()
                    .map(RankingItemResponse::from)
                    .toList();

            return new TopNResponse(items, date.format(DATE_FORMATTER), items.size());
        }
    }
}
