package com.loopers.interfaces.api.ranking;

import com.loopers.application.ranking.RankingInfo;
import java.util.List;
import java.util.stream.Collectors;

public class RankingV1Dto {

    public record DailyRankingListResponse(
            List<DailyRankingItem> rankings
    ) {
        public static DailyRankingListResponse from(RankingInfo rankingInfo) {
            List<DailyRankingItem> items = rankingInfo.items().stream()
                    .map(item -> new DailyRankingItem(
                            item.productId(),
                            item.productName(),
                            item.brandName(),
                            item.price(),
                            item.likeCount(),
                            item.rank().intValue()
                    ))
                    .collect(Collectors.toList());

            return new DailyRankingListResponse(items);
        }
    }

    public record DailyRankingItem(
            Long productId,
            String productName,
            String brandName,
            Integer price,
            Integer likeCount,
            Integer rank
    ) {
    }

}
