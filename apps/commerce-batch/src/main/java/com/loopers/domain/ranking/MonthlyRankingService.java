package com.loopers.domain.ranking;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MonthlyRankingService {

    private static final int TOP_RANKING_SIZE = 100;

    private final MvProductRankMonthlyRepository mvProductRankMonthlyRepository;

    @Transactional
    public void upsertMetrics(Long productId, RankingPeriod period, Double score, Long likeCount, Long viewCount, Long salesCount) {
        mvProductRankMonthlyRepository.findByProductIdAndPeriod(productId, period.startDate(), period.endDate())
                .ifPresentOrElse(
                        existing -> {
                            existing.updateMetrics(score, likeCount, viewCount, salesCount);
                            mvProductRankMonthlyRepository.save(existing);
                        },
                        () -> {
                            MvProductRankMonthly newRank = MvProductRankMonthly.create(
                                    productId,
                                    null,
                                    score,
                                    period.startDate(),
                                    period.endDate(),
                                    likeCount,
                                    viewCount,
                                    salesCount
                            );
                            mvProductRankMonthlyRepository.save(newRank);
                        }
                );
    }

    @Transactional
    public void calculateAndUpdateRanking(RankingPeriod period) {
        List<MvProductRankMonthly> allRanks = mvProductRankMonthlyRepository.findByPeriodOrderByRankingAsc(
                period.startDate(), period.endDate());

        if (allRanks.isEmpty()) {
            log.info("월간 랭킹 데이터가 없습니다: period={} ~ {}", period.startDate(), period.endDate());
            return;
        }

        // score 기준으로 정렬하여 TOP 100 추출
        List<MvProductRankMonthly> top100 = allRanks.stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(TOP_RANKING_SIZE)
                .toList();

        // TOP 100 상품의 랭킹만 업데이트
        int updatedCount = 0;
        for (int i = 0; i < top100.size(); i++) {
            MvProductRankMonthly rank = top100.get(i);
            rank.updateRanking(i + 1);
            mvProductRankMonthlyRepository.save(rank);
            updatedCount++;
        }

        // TOP 100에서 밀려난 기존 데이터 삭제
        Set<Long> top100ProductIds = top100.stream()
                .map(MvProductRankMonthly::getProductId)
                .collect(Collectors.toSet());

        int deletedCount = 0;
        for (MvProductRankMonthly existing : allRanks) {
            if (!top100ProductIds.contains(existing.getProductId())) {
                mvProductRankMonthlyRepository.delete(existing);
                deletedCount++;
            }
        }

        log.info("월간 랭킹 계산 완료: period={} ~ {}, updated={}, deleted={}",
                period.startDate(), period.endDate(), updatedCount, deletedCount);
    }
}

