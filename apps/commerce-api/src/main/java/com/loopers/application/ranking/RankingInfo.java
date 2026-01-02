package com.loopers.application.ranking;

import com.loopers.application.product.ProductInfo;
import com.loopers.domain.ranking.Ranking;

public record RankingInfo(
        int rank,
        Long productId,
        ProductInfo product,
        Double score
) {
    public static RankingInfo of(Ranking ranking, ProductInfo product) {
        return new RankingInfo(
                ranking.getRank(),
                ranking.getProductId(),
                product,
                ranking.getScore()
        );
    }

    public record ProductRankings(
            SimpleRankingInfo like,
            SimpleRankingInfo view,
            SimpleRankingInfo order,
            SimpleRankingInfo all
    ) {
        public static ProductRankings of(
                Ranking likeRanking,
                Ranking viewRanking,
                Ranking orderRanking,
                Ranking allRanking
        ) {
            // 모든 랭킹이 null이면 전체를 null로 반환
            if (likeRanking == null && viewRanking == null &&
                    orderRanking == null && allRanking == null) {
                return null;
            }

            return new ProductRankings(
                    SimpleRankingInfo.from(likeRanking),
                    SimpleRankingInfo.from(viewRanking),
                    SimpleRankingInfo.from(orderRanking),
                    SimpleRankingInfo.from(allRanking)
            );
        }
    }
}
