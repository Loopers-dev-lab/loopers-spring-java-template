package com.loopers.batch.ranking.recovery;

import com.loopers.batch.ranking.RankingType;
import com.loopers.batch.ranking.dto.ProductScore5MinDto;
import com.loopers.batch.ranking.step1.Aggregate5MinProcessor;
import com.loopers.batch.ranking.step2.RankingUpdateService;
import com.loopers.domain.ranking.ProductScore5Min;
import com.loopers.domain.ranking.RankingEventLog;
import com.loopers.infrastructure.ranking.ProductScore5MinJpaRepository;
import com.loopers.infrastructure.ranking.RankingEventLogJpaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Full Re-sync Tasklet (랭킹 타입별)
 * 
 * 전체 윈도우 기간의 RankingEventLog를 재집계하여
 * RankingScoreX를 덮어쓰기
 */
@Slf4j
public class FullResyncTasklet implements Tasklet {

    private final RankingType rankingType;
    private final RankingEventLogJpaRepository rankingEventLogJpaRepository;
    private final ProductScore5MinJpaRepository productScore5MinJpaRepository;
    private final RankingUpdateService rankingUpdateService;
    private final Aggregate5MinProcessor aggregate5MinProcessor;

    public FullResyncTasklet(RankingType rankingType,
                             RankingEventLogJpaRepository rankingEventLogJpaRepository,
                             ProductScore5MinJpaRepository productScore5MinJpaRepository,
                             RankingUpdateService rankingUpdateService,
                             Aggregate5MinProcessor aggregate5MinProcessor) {
        this.rankingType = rankingType;
        this.rankingEventLogJpaRepository = rankingEventLogJpaRepository;
        this.productScore5MinJpaRepository = productScore5MinJpaRepository;
        this.rankingUpdateService = rankingUpdateService;
        this.aggregate5MinProcessor = aggregate5MinProcessor;
    }

    @Override
    @Transactional
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("FullResyncTasklet 시작: type={}", rankingType);

        try {
            // 1. 윈도우 시작 시간 계산
            LocalDateTime windowEnd = LocalDateTime.now();
            LocalDateTime windowStart = calculateWindowStart(windowEnd);

            log.info("{} Full Re-sync: 윈도우 기간 {} ~ {}", rankingType, windowStart, windowEnd);

            // 2. 윈도우 기간의 RankingEventLog 조회
            List<RankingEventLog> eventLogs = rankingEventLogJpaRepository.findByOccurredAtBetween(
                windowStart, windowEnd
            );

            log.info("{} Full Re-sync: Found {} events between {} and {}", 
                rankingType, eventLogs.size(), windowStart, windowEnd);

            if (eventLogs.isEmpty()) {
                log.debug("{} Full Re-sync: No events found", rankingType);
                // 데이터가 없어도 RankingScoreX는 초기화해야 함
                rankingUpdateService.fullResyncRankingScore(rankingType, windowStart, windowEnd, productScore5MinJpaRepository);
                rankingUpdateService.recalculateScoresAndCreateSnapshot(rankingType, windowEnd);
                return RepeatStatus.FINISHED;
            }

            // 3. RankingEventLog를 5분 단위로 집계하여 ProductScore5Min 생성
            Map<String, ProductScore5MinDto> aggregated5Min = new HashMap<>();
            
            for (RankingEventLog eventLog : eventLogs) {
                try {
                    ProductScore5MinDto dto = aggregate5MinProcessor.process(eventLog);
                    if (dto != null) {
                        String key = createKey(dto.getProductId(), dto.getStartTime(), dto.getEndTime());
                        
                        ProductScore5MinDto existing = aggregated5Min.computeIfAbsent(key, k -> 
                            ProductScore5MinDto.builder()
                                .productId(dto.getProductId())
                                .startTime(dto.getStartTime())
                                .endTime(dto.getEndTime())
                                .orderAmountSum(BigDecimal.ZERO)
                                .likeCount(0L)
                                .viewCount(0L)
                                .build()
                        );
                        
                        existing.setOrderAmountSum(existing.getOrderAmountSum().add(dto.getOrderAmountSum()));
                        existing.setLikeCount(existing.getLikeCount() + dto.getLikeCount());
                        existing.setViewCount(existing.getViewCount() + dto.getViewCount());
                    }
                } catch (Exception e) {
                    log.warn("Failed to process event log: {}", eventLog.getId(), e);
                }
            }

            log.info("{} Full Re-sync: {}개의 5분 구간으로 집계 완료", rankingType, aggregated5Min.size());

            // 4. 윈도우 기간의 기존 ProductScore5Min 삭제 (해당 윈도우만)
            deleteProductScore5MinInWindow(windowStart, windowEnd);

            // 5. 새로운 ProductScore5Min 저장
            List<ProductScore5Min> entitiesToSave = aggregated5Min.values().stream()
                .map(dto -> ProductScore5Min.builder()
                    .productId(dto.getProductId())
                    .startTime(dto.getStartTime())
                    .endTime(dto.getEndTime())
                    .orderAmountSum(dto.getOrderAmountSum())
                    .likeCount(dto.getLikeCount())
                    .viewCount(dto.getViewCount())
                    .build())
                .toList();
            
            if (!entitiesToSave.isEmpty()) {
                productScore5MinJpaRepository.saveAll(entitiesToSave);
                log.info("{} Full Re-sync: {}건의 ProductScore5Min 저장 완료", rankingType, entitiesToSave.size());
            }

            // 6. ProductScore5Min을 기반으로 RankingScoreX 전체 재계산
            rankingUpdateService.fullResyncRankingScore(rankingType, windowStart, windowEnd, productScore5MinJpaRepository);

            // 7. 점수 재계산 및 스냅샷 생성
            rankingUpdateService.recalculateScoresAndCreateSnapshot(rankingType, windowEnd);

            log.info("FullResyncTasklet 완료: type={}, processed={}건", rankingType, eventLogs.size());
            return RepeatStatus.FINISHED;

        } catch (Exception e) {
            log.error("FullResyncTasklet 실패: type={}", rankingType, e);
            throw e;
        }
    }

    /**
     * 윈도우 기간의 ProductScore5Min 삭제
     */
    private void deleteProductScore5MinInWindow(LocalDateTime windowStart, LocalDateTime windowEnd) {
        List<ProductScore5Min> existingScores = productScore5MinJpaRepository.findByTimeRange(windowStart, windowEnd);
        if (!existingScores.isEmpty()) {
            productScore5MinJpaRepository.deleteAll(existingScores);
            log.info("{} Full Re-sync: {}건의 기존 ProductScore5Min 삭제 완료", rankingType, existingScores.size());
        }
    }

    /**
     * 윈도우 시작 시간 계산
     */
    private LocalDateTime calculateWindowStart(LocalDateTime targetTime) {
        return switch (rankingType) {
            case HOURLY -> targetTime.minusHours(1);
            case DAILY -> targetTime.minusDays(1);
            case WEEKLY -> targetTime.minusDays(7);
            case MONTHLY -> targetTime.minusDays(30);
        };
    }

    /**
     * (productId, startTime, endTime) 조합을 키로 변환
     */
    private String createKey(Long productId, LocalDateTime startTime, LocalDateTime endTime) {
        return String.format("%d_%s_%s", productId, startTime, endTime);
    }
}

