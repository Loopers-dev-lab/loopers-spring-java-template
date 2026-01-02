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
            log.info("저장할 주간 랭킹 데이터가 없습니다: yearWeek={}", yearWeek);
            return;
        }

        log.info("주간 랭킹 저장 시작: yearWeek={}, 저장할 데이터 수={}", yearWeek, items.size());

        try {
            // 1. 기존 데이터 삭제 (멱등성 보장)
            long deletedCount = weeklyRankRepository.deleteByYearWeek(yearWeek);
            log.info("기존 주간 랭킹 데이터 삭제: yearWeek={}, 삭제된 수={}", yearWeek, deletedCount);

            // 2. 새로운 데이터 저장
            List<WeeklyRankEntity> entities = items.stream()
                    .map(this::convertToEntity)
                    .toList();

            List<WeeklyRankEntity> savedEntities = weeklyRankRepository.saveAll(entities);
            log.info("주간 랭킹 저장 완료: yearWeek={}, 저장된 수={}", yearWeek, savedEntities.size());

        } catch (Exception e) {
            log.error("주간 랭킹 저장 중 오류 발생: yearWeek={}", yearWeek, e);
            throw new RuntimeException("주간 랭킹 저장 실패", e);
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