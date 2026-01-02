package com.loopers.application.ranking;

import com.loopers.domain.common.vo.Money;

import java.util.List;

public class RankingInfo {

    public record RankingPageInfo(
            List<RankingItemInfo> rankings,
            String date,
            int page,
            int size,
            long totalCount,
            int totalPages,
            boolean hasNext
    ) {
        public static RankingPageInfo of(
                List<RankingItemInfo> rankings,
                String date,
                int page,
                int size,
                long totalCount
        ) {
            int totalPages = size > 0 ? (int) Math.ceil((double) totalCount / size) : 0;
            boolean hasNext = page < totalPages - 1;
            return new RankingPageInfo(rankings, date, page, size, totalCount, totalPages, hasNext);
        }
    }

    public record RankingItemInfo(
            int rank,
            Long productId,
            String productName,
            String brandName,
            Money price,
            int likeCount,
            Double score
    ) {}
}
