package com.loopers.domain.ranking;

import com.loopers.infrastructure.ranking.RedisRankingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RankingService {

    private final RedisRankingRepository redisRankingRepository;

    /**
     * TOP N 랭킹 조회
     */
    public List<Ranking> getTopRanking(RankingType rankingType, LocalDate date, int limit) {
        if (limit <= 0 || limit > 100) {
            throw new IllegalArgumentException("limit은 1~100 사이여야 합니다: " + limit);
        }

        return redisRankingRepository.getTopRanking(rankingType, date, limit);
    }

    /**
     * 페이지네이션 랭킹 조회
     */
    public List<Ranking> getRankingWithPaging(RankingType rankingType, LocalDate date,
                                                   int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page는 0 이상이어야 합니다: " + page);
        }
        if (size <= 0 || size > 100) {
            throw new IllegalArgumentException("size는 1~100 사이여야 합니다: " + size);
        }

        int offset = page * size;
        return redisRankingRepository.getRankingWithPaging(rankingType, date, offset, size);
    }

    /**
     * 특정 상품의 랭킹 조회
     */
    public Ranking getProductRanking(RankingType rankingType, LocalDate date, Long productId) {
        return redisRankingRepository.getProductRanking(rankingType, date, productId);
    }

    /**
     * 전체 랭킹 개수
     */
    public long getTotalRankingCount(RankingType rankingType, LocalDate date) {
        return redisRankingRepository.getRankingSize(rankingType, date);
    }
}
