package com.loopers.ranking.streamer;

public record RankingInfo(
        Long id,
        Long score,
        Long rank
) {
    public static RankingInfo of(Long id, Long score, Long rank) {
        return new RankingInfo(
                id,
                score,
                rank
        );
    }
}
