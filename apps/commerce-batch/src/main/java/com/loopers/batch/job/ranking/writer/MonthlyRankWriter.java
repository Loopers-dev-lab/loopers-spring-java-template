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
            log.info("저장할 월간 랭킹 데이터가 없습니다: yearMonth={}", yearMonth);
            return;
        }

        log.info("월간 랭킹 저장 시작: yearMonth={}, 저장할 데이터 수={}", yearMonth, items.size());

        try {
            // 1. 기존 데이터 삭제 (멱등성 보장)
            long deletedCount = monthlyRankRepository.deleteByYearMonth(yearMonth);
            log.info("기존 월간 랭킹 데이터 삭제: yearMonth={}, 삭제된 수={}", yearMonth, deletedCount);

            // 2. 새로운 데이터 저장
            List<MonthlyRankEntity> entities = items.stream()
                    .map(this::convertToEntity)
                    .toList();

            List<MonthlyRankEntity> savedEntities = monthlyRankRepository.saveAll(entities);
            log.info("월간 랭킹 저장 완료: yearMonth={}, 저장된 수={}", yearMonth, savedEntities.size());

        } catch (Exception e) {
            log.error("월간 랭킹 저장 중 오류 발생: yearMonth={}", yearMonth, e);
            throw new RuntimeException("월간 랭킹 저장 실패", e);
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