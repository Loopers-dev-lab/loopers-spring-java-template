package com.loopers.interfaces.api.ranking;

import com.loopers.application.product.ProductInfo;
import com.loopers.application.ranking.RankingInfo;
import com.loopers.interfaces.api.product.ProductV1Dto;
import java.util.List;

public class RankingV1Dto {

    public record RankingResponse(
            List<RankingItemResponse> items,
            int page,
            int size,
            long total
    ) {
        public static RankingResponse from(List<RankingInfo> infos, int page, int size, long total) {
            List<RankingItemResponse> items = infos.stream()
                    .map(RankingItemResponse::from)
                    .toList();
            return new RankingResponse(items, page, size, total);
        }
    }

    public record RankingItemResponse(
            long rank,
            double score,
            ProductV1Dto.ProductResponseItem product
    ) {
        public static RankingItemResponse from(RankingInfo info) {
            return new RankingItemResponse(info.rank(), info.score(), ProductV1Dto.ProductResponseItem.from(info.product()));
        }
    }
}
