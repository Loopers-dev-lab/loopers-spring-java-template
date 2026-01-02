package com.loopers.interfaces.api.ranking;

import com.loopers.application.ranking.RankingInfo.RankingItemInfo;
import com.loopers.application.ranking.RankingInfo.RankingPageInfo;

import java.math.BigDecimal;
import java.util.List;

public class RankingV1Dto {

    public record RankingPageResponse(
            List<RankingItemResponse> rankings,
            String date,
            int page,  // 1-based page number for API response
            int size,
            long totalCount,
            int totalPages,
            boolean hasNext
    ) {
        public static RankingPageResponse from(RankingPageInfo info) {
            List<RankingItemResponse> rankings = info.rankings().stream()
                    .map(RankingItemResponse::from)
                    .toList();
            return new RankingPageResponse(
                    rankings,
                    info.date(),
                    info.page() + 1,  // 0-based → 1-based 변환
                    info.size(),
                    info.totalCount(),
                    info.totalPages(),
                    info.hasNext()
            );
        }
    }

    public record RankingItemResponse(
            int rank,
            Long productId,
            String productName,
            String brandName,
            BigDecimal price,
            int likeCount,
            Double score
    ) {
        public static RankingItemResponse from(RankingItemInfo info) {
            return new RankingItemResponse(
                    info.rank(),
                    info.productId(),
                    info.productName(),
                    info.brandName(),
                    info.price().getAmount(),
                    info.likeCount(),
                    info.score()
            );
        }
    }
}
