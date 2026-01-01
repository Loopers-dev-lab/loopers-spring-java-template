package com.loopers.interfaces.api.ranking;

import com.loopers.application.ranking.RankingInfo;
import java.util.List;
import java.util.stream.Collectors;

public class RankingV1Dto {

    public record RankingListResponse(
            List<RankingItem> rankings
    ) {
        public static RankingListResponse from(RankingInfo rankingInfo) {
            List<RankingItem> items = rankingInfo.items().stream()
                    .map(item -> new RankingItem(
                            item.productId(),
                            item.productName(),
                            item.brandName(),
                            item.price(),
                            item.likeCount(),
                            item.rank().intValue()
                    ))
                    .collect(Collectors.toList());

            return new RankingListResponse(items);
        }
    }

    public record RankingItem(
            Long productId,
            String productName,
            String brandName,
            Integer price,
            Integer likeCount,
            Integer rank
    ) {
    }

}
