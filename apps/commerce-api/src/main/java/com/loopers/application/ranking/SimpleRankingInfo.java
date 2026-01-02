package com.loopers.application.ranking;

import com.loopers.domain.ranking.Ranking;

public record SimpleRankingInfo(
        Integer rank,   // null 가능 (순위 없는 경우)
        Double score    // null 가능
) {
    public static SimpleRankingInfo from(Ranking ranking) {
        if (ranking == null) {
            return null;
        }
        return new SimpleRankingInfo(
                ranking.getRank(),
                ranking.getScore()
        );
    }
}
