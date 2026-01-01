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
public class WeeklyRankingService {

    private static final int TOP_RANKING_SIZE = 100;

    private final MvProductRankWeeklyRepository mvProductRankWeeklyRepository;

    @Transactional
    public void upsertMetrics(Long productId, RankingPeriod period, Double score, Long likeCount, Long viewCount, Long salesCount) {
        mvProductRankWeeklyRepository.findByProductIdAndPeriod(productId, period.startDate(), period.endDate())
                .ifPresentOrElse(
                        existing -> {
                            existing.updateMetrics(score, likeCount, viewCount, salesCount);
                            mvProductRankWeeklyRepository.save(existing);
                        },
                        () -> {
                            MvProductRankWeekly newRank = MvProductRankWeekly.create(
                                    productId,
                                    null,
                                    score,
                                    period.startDate(),
                                    period.endDate(),
                                    likeCount,
                                    viewCount,
                                    salesCount
                            );
                            mvProductRankWeeklyRepository.save(newRank);
                        }
                );
    }

    @Transactional
    public void calculateAndUpdateRanking(RankingPeriod period) {
        // MV 테이블에서 period에 해당하는 모든 데이터 조회
        List<MvProductRankWeekly> allRanks = mvProductRankWeeklyRepository.findByPeriodOrderByRankingAsc(
                period.startDate(), period.endDate());

        if (allRanks.isEmpty()) {
            log.info("주간 랭킹 데이터가 없습니다: period={} ~ {}", period.startDate(), period.endDate());
            return;
        }

        // score 기준으로 정렬하여 TOP 100 추출
        List<MvProductRankWeekly> top100 = allRanks.stream()
                .sorted((a, b) -> Double.compare(b.getScore(), a.getScore()))
                .limit(TOP_RANKING_SIZE)
                .toList();

        // TOP 100 상품의 랭킹만 업데이트
        int updatedCount = 0;
        for (int i = 0; i < top100.size(); i++) {
            MvProductRankWeekly rank = top100.get(i);
            rank.updateRanking(i + 1);
            mvProductRankWeeklyRepository.save(rank);
            updatedCount++;
        }

        // TOP 100에서 밀려난 기존 데이터 삭제
        Set<Long> top100ProductIds = top100.stream()
                .map(MvProductRankWeekly::getProductId)
                .collect(Collectors.toSet());

        int deletedCount = 0;
        for (MvProductRankWeekly existing : allRanks) {
            if (!top100ProductIds.contains(existing.getProductId())) {
                mvProductRankWeeklyRepository.delete(existing);
                deletedCount++;
            }
        }

        log.info("주간 랭킹 계산 완료: period={} ~ {}, updated={}, deleted={}",
                period.startDate(), period.endDate(), updatedCount, deletedCount);
    }
}

