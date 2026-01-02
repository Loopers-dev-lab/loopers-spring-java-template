package com.loopers.batch.job.ranking.writer;

import com.loopers.batch.job.ranking.dto.RankingAggregation;
import com.loopers.domain.ranking.WeeklyRankEntity;
import com.loopers.domain.ranking.WeeklyRankRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 주간 랭킹 Writer
 * - RankingAggregation을 WeeklyRankEntity로 변환하여 저장
 * - 멱등성 보장을 위해 기존 데이터 삭제 후 저장
 */
@Slf4j
@StepScope
@Component
@RequiredArgsConstructor
public class WeeklyRankWriter implements ItemWriter<RankingAggregation> {

    private final WeeklyRankRepository weeklyRankRepository;

    @Value("#{jobParameters['yearWeek']}")
    private String yearWeek;

    @Override
    public void write(Chunk<? extends RankingAggregation> chunk) throws Exception {
        List<? extends RankingAggregation> items = chunk.getItems();

        if (items.isEmpty()) {
            log.info("[Batch-Ranking] 저장할 주간 랭킹 데이터가 없습니다. (yearWeek: {})", yearWeek);
            return;
        }

        int targetCount = items.size();
        log.info("[Batch-Ranking] 주간 랭킹 저장 프로세스 시작 (yearWeek: {}, 대상 건수: {})", yearWeek, targetCount);

        try {
            // 1. 기존 데이터 삭제 (멱등성 보장)
            long deletedCount = weeklyRankRepository.deleteByYearWeek(yearWeek);
            log.info("[Batch-Ranking] 기존 데이터 클렌징 완료 (yearWeek: {}, 삭제 건수: {})", yearWeek, deletedCount);

            // 2. 새로운 데이터 저장
            List<WeeklyRankEntity> entities = items.stream()
                    .map(this::convertToEntity)
                    .toList();

            weeklyRankRepository.saveAll(entities);
            log.info("[Batch-Ranking] 주간 랭킹 저장 성공 (yearWeek: {}, 저장 건수: {})", yearWeek, entities.size());

        } catch (Exception e) {
            // 상세한 컨텍스트를 포함한 에러 로그
            log.error("[Batch-Ranking] 주간 랭킹 저장 중 예외 발생! (yearWeek: {}, 처리 중이던 건수: {}) - 원인: {}",
                    yearWeek, targetCount, e.getMessage(), e);
            throw e; // 예외를 그대로 던져서 Batch Step이 실패 상태가 되도록 위임
        }
    }

    private WeeklyRankEntity convertToEntity(RankingAggregation aggregation) {
        return WeeklyRankEntity.create(
                aggregation.getProductId(),
                yearWeek,
                aggregation.getViewCount(),
                aggregation.getLikeCount(),
                aggregation.getSalesCount(),
                aggregation.getOrderCount(),
                aggregation.getTotalScore(),
                aggregation.getRankPosition()
        );
    }
}
