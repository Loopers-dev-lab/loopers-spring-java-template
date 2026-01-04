package com.loopers.batch.ranking.step2;

import com.loopers.batch.ranking.RankingType;
import com.loopers.batch.ranking.dto.RankingUpdateData;
import com.loopers.config.ranking.RankingProperties;
import com.loopers.domain.ranking.ProductScore5Min;
import com.loopers.domain.ranking.RankingEventType;
import com.loopers.domain.ranking.RankingScoreDaily;
import com.loopers.domain.ranking.RankingScoreHourly;
import com.loopers.domain.ranking.RankingScoreMonthly;
import com.loopers.domain.ranking.RankingScoreWeekly;
import com.loopers.domain.ranking.RankingSnapshotDaily;
import com.loopers.domain.ranking.RankingSnapshotHourly;
import com.loopers.domain.ranking.RankingSnapshotMonthly;
import com.loopers.domain.ranking.RankingSnapshotWeekly;
import com.loopers.domain.ranking.RankingWeightPolicy;
import com.loopers.domain.ranking.RankingWeightPolicyRepository;
import com.loopers.infrastructure.ranking.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Step 2: 랭킹 업데이트 서비스
 * UPSERT, 점수 재계산, 스냅샷 생성을 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankingUpdateService {

    private final RankingScoreHourlyJpaRepository hourlyRepository;
    private final RankingScoreDailyJpaRepository dailyRepository;
    private final RankingScoreWeeklyJpaRepository weeklyRepository;
    private final RankingScoreMonthlyJpaRepository monthlyRepository;
    
    private final RankingSnapshotHourlyJpaRepository hourlySnapshotRepository;
    private final RankingSnapshotDailyJpaRepository dailySnapshotRepository;
    private final RankingSnapshotWeeklyJpaRepository weeklySnapshotRepository;
    private final RankingSnapshotMonthlyJpaRepository monthlySnapshotRepository;
    
    private final RankingWeightPolicyRepository weightPolicyRepository;
    private final RankingProperties rankingProperties;

    private static final int BATCH_SIZE = 1000;
    private static final int SNAPSHOT_LIMIT = 100; // TOP 100 랭킹만 저장 (Materialized View)

    /**
     * 배치 UPSERT 처리 (1000건씩 청크)
     */
    @Transactional
    public void batchUpsert(RankingType rankingType, Collection<RankingUpdateData> updateDataList, LocalDateTime processedTime) {
        if (updateDataList.isEmpty()) {
            return;
        }

        List<RankingUpdateData> dataList = new ArrayList<>(updateDataList);
        
        // 배치 단위로 처리 (1000건씩)
        for (int i = 0; i < dataList.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, dataList.size());
            List<RankingUpdateData> batch = dataList.subList(i, end);
            
            for (RankingUpdateData data : batch) {
                upsertSingle(rankingType, data, processedTime);
            }
        }

        log.debug("{} UPSERT 완료: {}건", rankingType, dataList.size());
    }

    /**
     * 단일 UPSERT 처리
     */
    private void upsertSingle(RankingType rankingType, RankingUpdateData data, LocalDateTime processedTime) {
        // 기존 데이터 조회하여 현재 누적값 계산
        BigDecimal currentTotalOrderAmount = getCurrentTotalOrderAmount(rankingType, data.getProductId())
            .orElse(BigDecimal.ZERO);
        Long currentTotalLikeCount = getCurrentTotalLikeCount(rankingType, data.getProductId()).orElse(0L);
        Long currentTotalViewCount = getCurrentTotalViewCount(rankingType, data.getProductId()).orElse(0L);

        // 슬라이딩 윈도우 업데이트: NEW 추가, OLD 제거
        BigDecimal newTotalOrderAmount = currentTotalOrderAmount
            .add(data.getNewOrderAmount())
            .subtract(data.getOldOrderAmount());
        Long newTotalLikeCount = currentTotalLikeCount + data.getNewLikeCount() - data.getOldLikeCount();
        Long newTotalViewCount = currentTotalViewCount + data.getNewViewCount() - data.getOldViewCount();

        // 점수 계산
        Double currentScore = calculateScore(newTotalOrderAmount, newTotalLikeCount, newTotalViewCount);

        // UPSERT 실행
        upsertRankingScore(rankingType, data.getProductId(), newTotalOrderAmount, 
            newTotalLikeCount, newTotalViewCount, currentScore, processedTime,
            data.getNewOrderAmount(), data.getOldOrderAmount(),
            data.getNewLikeCount(), data.getOldLikeCount(),
            data.getNewViewCount(), data.getOldViewCount());
    }

    /**
     * 점수 재계산 및 스냅샷 생성
     */
    @Transactional
    public void recalculateScoresAndCreateSnapshot(RankingType rankingType, LocalDateTime snapshotTime) {
        // 1. 상위 100개 조회
        List<?> topScores = getTopScores(rankingType, SNAPSHOT_LIMIT);
        
        if (topScores.isEmpty()) {
            log.debug("{} 스냅샷 생성 스킵: 데이터 없음", rankingType);
            return;
        }

        // 2. 스냅샷 생성 (rank 할당)
        int rank = 1;
        for (Object scoreObj : topScores) {
            Long productId = getProductId(scoreObj);
            Double totalScore = getCurrentScore(scoreObj);
            
            createSnapshot(rankingType, productId, rank, totalScore, snapshotTime);
            rank++;
        }

        log.debug("{} 스냅샷 생성 완료: {}건", rankingType, topScores.size());
    }

    /**
     * 점수 계산: log10(total_order_amount + 1) * w1 + total_like_count * w2 + total_view_count * w3
     */
    private Double calculateScore(BigDecimal totalOrderAmount, Long totalLikeCount, Long totalViewCount) {
        // Weight 조회
        double orderWeight = getWeight(RankingEventType.ORDER);
        double likeWeight = getWeight(RankingEventType.LIKE);
        double viewWeight = getWeight(RankingEventType.VIEW);

        // log10(total_order_amount + 1) * weight
        double orderScore = Math.log10(totalOrderAmount.doubleValue() + 1.0) * orderWeight;
        
        // total_like_count * weight
        double likeScore = totalLikeCount * likeWeight;
        
        // total_view_count * weight
        double viewScore = totalViewCount * viewWeight;

        return orderScore + likeScore + viewScore;
    }

    /**
     * Weight 조회 (DB 또는 기본값)
     */
    private double getWeight(RankingEventType eventType) {
        return weightPolicyRepository.findByEventType(eventType)
            .filter(RankingWeightPolicy::getIsActive)
            .map(RankingWeightPolicy::getWeight)
            .orElseGet(() -> getDefaultWeight(eventType));
    }

    /**
     * 기본 Weight (fallback)
     */
    private double getDefaultWeight(RankingEventType eventType) {
        if (rankingProperties == null || rankingProperties.weights() == null) {
            // 기본값 반환
            return switch (eventType) {
                case ORDER -> 1.0;
                case LIKE -> 1.0;
                case VIEW -> 1.0;
            };
        }
        
        return switch (eventType) {
            case ORDER -> rankingProperties.weights().order();
            case LIKE -> rankingProperties.weights().like();
            case VIEW -> rankingProperties.weights().view();
        };
    }

    /**
     * UPSERT 실행 (공통 메서드)
     */
    private void upsertRankingScore(RankingType rankingType, Long productId, 
                                   BigDecimal newTotalOrderAmount, Long newTotalLikeCount, Long newTotalViewCount,
                                   Double currentScore, LocalDateTime processedTime,
                                   BigDecimal newOrderAmount, BigDecimal oldOrderAmount,
                                   Long newLikeCount, Long oldLikeCount,
                                   Long newViewCount, Long oldViewCount) {
        switch (rankingType) {
            case HOURLY -> hourlyRepository.upsertRankingScore(productId, newTotalOrderAmount, 
                newTotalLikeCount, newTotalViewCount, currentScore, processedTime,
                newOrderAmount, oldOrderAmount, newLikeCount, oldLikeCount, newViewCount, oldViewCount);
            case DAILY -> dailyRepository.upsertRankingScore(productId, newTotalOrderAmount, 
                newTotalLikeCount, newTotalViewCount, currentScore, processedTime,
                newOrderAmount, oldOrderAmount, newLikeCount, oldLikeCount, newViewCount, oldViewCount);
            case WEEKLY -> weeklyRepository.upsertRankingScore(productId, newTotalOrderAmount, 
                newTotalLikeCount, newTotalViewCount, currentScore, processedTime,
                newOrderAmount, oldOrderAmount, newLikeCount, oldLikeCount, newViewCount, oldViewCount);
            case MONTHLY -> monthlyRepository.upsertRankingScore(productId, newTotalOrderAmount, 
                newTotalLikeCount, newTotalViewCount, currentScore, processedTime,
                newOrderAmount, oldOrderAmount, newLikeCount, oldLikeCount, newViewCount, oldViewCount);
        }
    }

    // Helper methods for getting current values
    private Optional<BigDecimal> getCurrentTotalOrderAmount(RankingType rankingType, Long productId) {
        return switch (rankingType) {
            case HOURLY -> hourlyRepository.findById(productId).map(r -> r.getTotalOrderAmount());
            case DAILY -> dailyRepository.findById(productId).map(r -> r.getTotalOrderAmount());
            case WEEKLY -> weeklyRepository.findById(productId).map(r -> r.getTotalOrderAmount());
            case MONTHLY -> monthlyRepository.findById(productId).map(r -> r.getTotalOrderAmount());
        };
    }

    private Optional<Long> getCurrentTotalLikeCount(RankingType rankingType, Long productId) {
        return switch (rankingType) {
            case HOURLY -> hourlyRepository.findById(productId).map(r -> r.getTotalLikeCount());
            case DAILY -> dailyRepository.findById(productId).map(r -> r.getTotalLikeCount());
            case WEEKLY -> weeklyRepository.findById(productId).map(r -> r.getTotalLikeCount());
            case MONTHLY -> monthlyRepository.findById(productId).map(r -> r.getTotalLikeCount());
        };
    }

    private Optional<Long> getCurrentTotalViewCount(RankingType rankingType, Long productId) {
        return switch (rankingType) {
            case HOURLY -> hourlyRepository.findById(productId).map(r -> r.getTotalViewCount());
            case DAILY -> dailyRepository.findById(productId).map(r -> r.getTotalViewCount());
            case WEEKLY -> weeklyRepository.findById(productId).map(r -> r.getTotalViewCount());
            case MONTHLY -> monthlyRepository.findById(productId).map(r -> r.getTotalViewCount());
        };
    }

    private List<?> getTopScores(RankingType rankingType, int limit) {
        return switch (rankingType) {
            case HOURLY -> hourlyRepository.findTopByOrderByCurrentScoreDesc(limit);
            case DAILY -> dailyRepository.findTopByOrderByCurrentScoreDesc(limit);
            case WEEKLY -> weeklyRepository.findTopByOrderByCurrentScoreDesc(limit);
            case MONTHLY -> monthlyRepository.findTopByOrderByCurrentScoreDesc(limit);
        };
    }

    private Long getProductId(Object scoreObj) {
        if (scoreObj instanceof RankingScoreHourly hourly) {
            return hourly.getProductId();
        } else if (scoreObj instanceof RankingScoreDaily daily) {
            return daily.getProductId();
        } else if (scoreObj instanceof RankingScoreWeekly weekly) {
            return weekly.getProductId();
        } else if (scoreObj instanceof RankingScoreMonthly monthly) {
            return monthly.getProductId();
        } else {
            throw new IllegalArgumentException("Unknown score type: " + scoreObj.getClass());
        }
    }

    private Double getCurrentScore(Object scoreObj) {
        if (scoreObj instanceof RankingScoreHourly hourly) {
            return hourly.getCurrentScore();
        } else if (scoreObj instanceof RankingScoreDaily daily) {
            return daily.getCurrentScore();
        } else if (scoreObj instanceof RankingScoreWeekly weekly) {
            return weekly.getCurrentScore();
        } else if (scoreObj instanceof RankingScoreMonthly monthly) {
            return monthly.getCurrentScore();
        } else {
            throw new IllegalArgumentException("Unknown score type: " + scoreObj.getClass());
        }
    }

    private void createSnapshot(RankingType rankingType, Long productId, Integer rank, Double totalScore, LocalDateTime snapshotTime) {
        switch (rankingType) {
            case HOURLY -> hourlySnapshotRepository.save(
                RankingSnapshotHourly.builder()
                    .productId(productId).productRank(rank).totalScore(totalScore).snapshotTime(snapshotTime).build());
            case DAILY -> dailySnapshotRepository.save(
                RankingSnapshotDaily.builder()
                    .productId(productId).productRank(rank).totalScore(totalScore).snapshotTime(snapshotTime).build());
            case WEEKLY -> weeklySnapshotRepository.save(
                RankingSnapshotWeekly.builder()
                    .productId(productId).productRank(rank).totalScore(totalScore).snapshotTime(snapshotTime).build());
            case MONTHLY -> monthlySnapshotRepository.save(
                RankingSnapshotMonthly.builder()
                    .productId(productId).productRank(rank).totalScore(totalScore).snapshotTime(snapshotTime).build());
        }
    }

    /**
     * Full Re-sync: 전체 윈도우 기간 재집계 및 덮어쓰기
     * ProductScore5Min을 기반으로 RankingScoreX 전체 재계산
     */
    @Transactional
    public void fullResyncRankingScore(RankingType rankingType, 
                                       LocalDateTime windowStart, 
                                       LocalDateTime windowEnd,
                                       ProductScore5MinJpaRepository productScore5MinJpaRepository) {
        log.info("{} Full Re-sync 시작: windowStart={}, windowEnd={}", rankingType, windowStart, windowEnd);

        // 1. 기존 RankingScoreX 전체 삭제
        deleteAllRankingScores(rankingType);
        log.info("{} 기존 데이터 삭제 완료", rankingType);

        // 2. 윈도우 기간의 ProductScore5Min 조회
        List<ProductScore5Min> productScores = productScore5MinJpaRepository
            .findByTimeRange(windowStart, windowEnd);

        if (productScores.isEmpty()) {
            log.info("{} Full Re-sync: ProductScore5Min 데이터 없음", rankingType);
            return;
        }

        log.info("{} Full Re-sync: {}건의 ProductScore5Min 데이터 발견", rankingType, productScores.size());

        // 3. 상품별로 Raw Metrics 집계
        Map<Long, RawMetrics> aggregatedMetrics = new HashMap<>();
        for (ProductScore5Min score : productScores) {
            Long productId = score.getProductId();
            RawMetrics existing = aggregatedMetrics.get(productId);
            
            if (existing == null) {
                aggregatedMetrics.put(productId, new RawMetrics(
                    score.getOrderAmountSum(),
                    score.getLikeCount(),
                    score.getViewCount()
                ));
            } else {
                aggregatedMetrics.put(productId, new RawMetrics(
                    existing.totalOrderAmount().add(score.getOrderAmountSum()),
                    existing.totalLikeCount() + score.getLikeCount(),
                    existing.totalViewCount() + score.getViewCount()
                ));
            }
        }

        log.info("{} Full Re-sync: {}개 상품 집계 완료", rankingType, aggregatedMetrics.size());

        // 4. RankingScoreX에 저장 (배치 처리)
        List<RankingScoreHourly> hourlyScores = new ArrayList<>();
        List<RankingScoreDaily> dailyScores = new ArrayList<>();
        List<RankingScoreWeekly> weeklyScores = new ArrayList<>();
        List<RankingScoreMonthly> monthlyScores = new ArrayList<>();

        for (Map.Entry<Long, RawMetrics> entry : aggregatedMetrics.entrySet()) {
            Long productId = entry.getKey();
            RawMetrics metrics = entry.getValue();
            
            Double currentScore = calculateScore(metrics.totalOrderAmount(), metrics.totalLikeCount(), metrics.totalViewCount());

            switch (rankingType) {
                case HOURLY -> hourlyScores.add(RankingScoreHourly.builder()
                    .productId(productId).totalOrderAmount(metrics.totalOrderAmount())
                    .totalLikeCount(metrics.totalLikeCount()).totalViewCount(metrics.totalViewCount())
                    .currentScore(currentScore).lastProcessedTime(windowEnd).build());
                case DAILY -> dailyScores.add(RankingScoreDaily.builder()
                    .productId(productId).totalOrderAmount(metrics.totalOrderAmount())
                    .totalLikeCount(metrics.totalLikeCount()).totalViewCount(metrics.totalViewCount())
                    .currentScore(currentScore).lastProcessedTime(windowEnd).build());
                case WEEKLY -> weeklyScores.add(RankingScoreWeekly.builder()
                    .productId(productId).totalOrderAmount(metrics.totalOrderAmount())
                    .totalLikeCount(metrics.totalLikeCount()).totalViewCount(metrics.totalViewCount())
                    .currentScore(currentScore).lastProcessedTime(windowEnd).build());
                case MONTHLY -> monthlyScores.add(RankingScoreMonthly.builder()
                    .productId(productId).totalOrderAmount(metrics.totalOrderAmount())
                    .totalLikeCount(metrics.totalLikeCount()).totalViewCount(metrics.totalViewCount())
                    .currentScore(currentScore).lastProcessedTime(windowEnd).build());
            }
        }

        // 5. 배치 저장
        int savedCount = switch (rankingType) {
            case HOURLY -> {
                hourlyRepository.saveAll(hourlyScores);
                yield hourlyScores.size();
            }
            case DAILY -> {
                dailyRepository.saveAll(dailyScores);
                yield dailyScores.size();
            }
            case WEEKLY -> {
                weeklyRepository.saveAll(weeklyScores);
                yield weeklyScores.size();
            }
            case MONTHLY -> {
                monthlyRepository.saveAll(monthlyScores);
                yield monthlyScores.size();
            }
        };
        log.info("{} Full Re-sync: {}건 저장 완료", rankingType, savedCount);

        log.info("{} Full Re-sync 완료", rankingType);
    }

    /**
     * 기존 RankingScoreX 전체 삭제
     */
    private void deleteAllRankingScores(RankingType rankingType) {
        switch (rankingType) {
            case HOURLY -> hourlyRepository.deleteAll();
            case DAILY -> dailyRepository.deleteAll();
            case WEEKLY -> weeklyRepository.deleteAll();
            case MONTHLY -> monthlyRepository.deleteAll();
        }
    }

    /**
     * Raw Metrics 집계용 record
     */
    private record RawMetrics(
        BigDecimal totalOrderAmount,
        Long totalLikeCount,
        Long totalViewCount
    ) {
        RawMetrics {
            if (totalOrderAmount == null) {
                throw new IllegalArgumentException("totalOrderAmount는 필수입니다");
            }
            if (totalLikeCount == null) {
                throw new IllegalArgumentException("totalLikeCount는 필수입니다");
            }
            if (totalViewCount == null) {
                throw new IllegalArgumentException("totalViewCount는 필수입니다");
            }
        }
    }
}

