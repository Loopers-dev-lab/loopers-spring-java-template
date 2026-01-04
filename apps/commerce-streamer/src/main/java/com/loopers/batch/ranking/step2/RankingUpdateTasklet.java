package com.loopers.batch.ranking.step2;

import com.loopers.batch.ranking.RankingType;
import com.loopers.batch.ranking.dto.RankingUpdateData;
import com.loopers.domain.ranking.ProductScore5Min;
import com.loopers.infrastructure.ranking.ProductScore5MinJpaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Step 2: 슬라이딩 윈도우 업데이트 Tasklet (랭킹 타입별)
 * 
 * 이 Tasklet은 각 랭킹 타입(Hourly, Daily, Weekly, Monthly)별로 실행됩니다.
 * 병렬 처리는 Job Configuration에서 Parallel Flow로 구성됩니다.
 */
@Slf4j
public class RankingUpdateTasklet implements Tasklet {

    private final RankingType rankingType;
    private final ProductScore5MinJpaRepository productScore5MinJpaRepository;
    private final RankingUpdateService rankingUpdateService;

    public RankingUpdateTasklet(RankingType rankingType,
                                 ProductScore5MinJpaRepository productScore5MinJpaRepository,
                                 RankingUpdateService rankingUpdateService) {
        this.rankingType = rankingType;
        this.productScore5MinJpaRepository = productScore5MinJpaRepository;
        this.rankingUpdateService = rankingUpdateService;
    }

    @Override
    @Transactional
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("RankingUpdateTasklet 시작: type={}", rankingType);

        try {
            // 1. 현재 처리할 5분 구간 파악
            LocalDateTime targetTime = LocalDateTime.now().minusMinutes(2); // Late-Arriving Data 버퍼
            LocalDateTime windowStart = calculateWindowStart(targetTime);
            LocalDateTime windowEnd = targetTime;

            // 2. NEW_RAW_DATA 조회 (현재 5분 구간)
            LocalDateTime newStart = truncateTo5MinuteInterval(targetTime.minusMinutes(5));
            LocalDateTime newEnd = newStart.plusMinutes(5);

            List<ProductScore5Min> newRawData = productScore5MinJpaRepository.findByTimeRange(newStart, newEnd);
            log.debug("{} NEW_RAW_DATA: {}건", rankingType, newRawData.size());

            // 3. OLD_RAW_DATA 조회 (윈도우 밖으로 밀려난 구간)
            LocalDateTime oldStart = truncateTo5MinuteInterval(windowStart.minusMinutes(5));
            LocalDateTime oldEnd = oldStart.plusMinutes(5);

            List<ProductScore5Min> oldRawData = productScore5MinJpaRepository.findByTimeRange(oldStart, oldEnd);
            log.debug("{} OLD_RAW_DATA: {}건", rankingType, oldRawData.size());

            // 4. 슬라이딩 윈도우 업데이트 데이터 생성
            Map<Long, RankingUpdateData> updateDataMap = buildUpdateDataMap(newRawData, oldRawData, targetTime);

            // 5. 배치 UPSERT (1000건씩 청크 처리)
            rankingUpdateService.batchUpsert(rankingType, updateDataMap.values(), targetTime);

            // 6. 점수 재계산 및 스냅샷 생성
            rankingUpdateService.recalculateScoresAndCreateSnapshot(rankingType, targetTime);

            log.info("RankingUpdateTasklet 완료: type={}, processed={}건", rankingType, updateDataMap.size());
            return RepeatStatus.FINISHED;

        } catch (Exception e) {
            log.error("RankingUpdateTasklet 실패: type={}", rankingType, e);
            throw e;
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
     * NEW와 OLD 데이터를 조합하여 업데이트 데이터 맵 생성
     */
    private Map<Long, RankingUpdateData> buildUpdateDataMap(
            List<ProductScore5Min> newRawData, 
            List<ProductScore5Min> oldRawData,
            LocalDateTime processedTime) {
        
        Map<Long, RankingUpdateData> updateMap = new HashMap<>();

        // NEW 데이터 처리
        for (ProductScore5Min newData : newRawData) {
            Long productId = newData.getProductId();
            updateMap.put(productId, RankingUpdateData.fromNewData(
                productId,
                newData.getOrderAmountSum(),
                newData.getLikeCount(),
                newData.getViewCount(),
                processedTime
            ));
        }

        // OLD 데이터 처리 (기존 데이터에 OLD 값 추가)
        for (ProductScore5Min oldData : oldRawData) {
            Long productId = oldData.getProductId();
            RankingUpdateData existing = updateMap.get(productId);
            
            if (existing != null) {
                // NEW와 OLD가 모두 있는 경우
                updateMap.put(productId, RankingUpdateData.fromSlidingWindow(
                    productId,
                    existing.getNewOrderAmount(),
                    existing.getNewLikeCount(),
                    existing.getNewViewCount(),
                    oldData.getOrderAmountSum(),
                    oldData.getLikeCount(),
                    oldData.getViewCount(),
                    processedTime
                ));
            } else {
                // OLD만 있는 경우 (윈도우 밖으로 밀려난 상품)
                updateMap.put(productId, RankingUpdateData.fromSlidingWindow(
                    productId,
                    BigDecimal.ZERO,
                    0L,
                    0L,
                    oldData.getOrderAmountSum(),
                    oldData.getLikeCount(),
                    oldData.getViewCount(),
                    processedTime
                ));
            }
        }

        return updateMap;
    }

    /**
     * 시간을 5분 단위로 내림 처리
     */
    private LocalDateTime truncateTo5MinuteInterval(LocalDateTime time) {
        return time.truncatedTo(ChronoUnit.MINUTES)
            .withMinute((time.getMinute() / 5) * 5)
            .withSecond(0)
            .withNano(0);
    }
}

