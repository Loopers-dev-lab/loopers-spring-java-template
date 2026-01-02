package com.loopers.batch.job.ranking.writer;

import com.loopers.batch.job.ranking.dto.RankingAggregation;
import com.loopers.domain.ranking.MonthlyRankEntity;
import com.loopers.domain.ranking.MonthlyRankRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 월간 랭킹 Writer
 * - RankingAggregation을 MonthlyRankEntity로 변환하여 저장
 * - 멱등성 보장을 위해 기존 데이터 삭제 후 저장
 */
@Slf4j
@StepScope
@Component
@RequiredArgsConstructor
public class MonthlyRankWriter implements ItemWriter<RankingAggregation> {

    private final MonthlyRankRepository monthlyRankRepository;

    @Value("#{jobParameters['yearMonth']}")
    private String yearMonth;

    @Override
    public void write(Chunk<? extends RankingAggregation> chunk) throws Exception {
        List<? extends RankingAggregation> items = chunk.getItems();

        if (items.isEmpty()) {
            log.info("[Batch-Ranking] 저장할 월간 랭킹 데이터가 없습니다. (yearMonth: {})", yearMonth);
            return;
        }

        int targetCount = items.size();
        log.info("[Batch-Ranking] 월간 랭킹 저장 프로세스 시작 (yearMonth: {}, 대상 건수: {})", yearMonth, targetCount);

        try {
            // 1. 기존 데이터 삭제 (멱등성 보장)
            long deletedCount = monthlyRankRepository.deleteByYearMonth(yearMonth);
            log.info("[Batch-Ranking] 기존 데이터 클렌징 완료 (yearMonth: {}, 삭제 건수: {})", yearMonth, deletedCount);

            // 2. 새로운 데이터 저장
            List<MonthlyRankEntity> entities = items.stream()
                    .map(this::convertToEntity)
                    .toList();

            monthlyRankRepository.saveAll(entities);
            log.info("[Batch-Ranking] 월간 랭킹 저장 성공 (yearMonth: {}, 저장 건수: {})", yearMonth, entities.size());

        } catch (Exception e) {
            // 상세한 컨텍스트를 포함한 에러 로그
            log.error("[Batch-Ranking] 월간 랭킹 저장 중 예외 발생! (yearMonth: {}, 처리 중이던 건수: {}) - 원인: {}",
                    yearMonth, targetCount, e.getMessage(), e);
            throw e;
        }
    }

    private MonthlyRankEntity convertToEntity(RankingAggregation aggregation) {
        return MonthlyRankEntity.create(
                aggregation.getProductId(),
                yearMonth,
                aggregation.getViewCount(),
                aggregation.getLikeCount(),
                aggregation.getSalesCount(),
                aggregation.getOrderCount(),
                aggregation.getTotalScore(),
                aggregation.getRankPosition()
        );
    }
}
