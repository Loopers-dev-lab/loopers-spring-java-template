package com.loopers.application.ranking;

import com.loopers.domain.ranking.RankingEventLogRepository;
import com.loopers.domain.ranking.RankingEventType;
import com.loopers.domain.ranking.RankingSnapshotHourly;
import com.loopers.domain.ranking.RankingSnapshotDaily;
import com.loopers.domain.ranking.RankingSnapshotHourlyRepository;
import com.loopers.domain.ranking.RankingSnapshotDailyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 랭킹 스냅샷 서비스
 * 스냅샷 생성 및 Redis 동기화를 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankingSnapshotService {

    private final RankingEventLogRepository rankingEventLogRepository;
    private final RankingSnapshotHourlyRepository rankingSnapshotHourlyRepository;
    private final RankingSnapshotDailyRepository rankingSnapshotDailyRepository;
    private final StringRedisTemplate redisTemplate;
    private final RankingWeightService rankingWeightService;

    private static final String RANKING_HOURLY_KEY = "ranking:hourly";
    private static final String RANKING_DAILY_KEY = "ranking:daily";
    private static final String RANKING_HOURLY_TEMP_KEY = "ranking:hourly:temp";
    private static final String RANKING_DAILY_TEMP_KEY = "ranking:daily:temp";

    /**
     * 시간 단위 스냅샷 생성
     * 특정 시간대(예: 10:00)의 이벤트 로그를 집계하여 Hourly 스냅샷에 저장
     * 동적 weight를 적용하여 점수 계산
     * 주/월별 랭킹 집계를 위한 확정 데이터 저장
     */
    @Transactional
    public void createHourlySnapshot(LocalDateTime windowEnd) {
        // windowEnd를 시간 단위로 정규화 (예: 10:05 → 10:00)
        LocalDateTime snapshotTime = windowEnd.withMinute(0).withSecond(0).withNano(0);
        // 해당 시간대의 시작 시각 (예: 10:00)
        LocalDateTime hourStart = snapshotTime;
        // 해당 시간대의 종료 시각 (예: 10:59:59.999)
        LocalDateTime hourEnd = snapshotTime.plusHours(1).minusNanos(1);
        
        log.info("Creating hourly snapshot for time range: {} ~ {} (hourly aggregate)", hourStart, hourEnd);
        
        // 해당 시간대의 이벤트 로그를 이벤트 타입별로 집계
        List<Object[]> aggregates = rankingEventLogRepository
            .aggregateByProductIdAndEventTypeAndTimeRange(hourStart, hourEnd);
        
        if (aggregates.isEmpty()) {
            log.debug("No events found for hour: {}, skipping snapshot creation", snapshotTime);
            return;
        }
        
        // 상품별로 점수 집계 (동적 weight 적용)
        Map<Long, Double> productScores = new HashMap<>();
        for (Object[] aggregate : aggregates) {
            Long productId = (Long) aggregate[0];
            RankingEventType eventType = (RankingEventType) aggregate[1];
            Long eventCount = ((Number) aggregate[2]).longValue();
            BigDecimal sumRawPrice = aggregate[3] != null ? 
                (aggregate[3] instanceof BigDecimal ? (BigDecimal) aggregate[3] : 
                 BigDecimal.valueOf(((Number) aggregate[3]).doubleValue())) : BigDecimal.ZERO;
            Long sumRawQuantity = aggregate[4] != null ? 
                ((Number) aggregate[4]).longValue() : 0L;
            Double sumOrderScore = aggregate[5] != null ? 
                ((Number) aggregate[5]).doubleValue() : 0.0;
            
            if (productId == null || eventType == null) {
                continue;
            }
            
            // 동적 weight 조회
            double weight = rankingWeightService.getWeight(eventType);
            
            // 이벤트 타입별 점수 계산
            double score = calculateScoreByEventType(eventType, eventCount, sumRawPrice, sumRawQuantity, sumOrderScore, weight);
            
            productScores.merge(productId, score, Double::sum);
        }
        
        // 스냅샷 저장 (기존 스냅샷이 있으면 삭제 후 새로 생성)
        int savedCount = 0;
        for (Map.Entry<Long, Double> entry : productScores.entrySet()) {
            Long productId = entry.getKey();
            Double totalScore = entry.getValue();
            
            // 기존 스냅샷이 있으면 삭제
            rankingSnapshotHourlyRepository
                .findByProductIdAndSnapshotTime(productId, snapshotTime)
                .ifPresent(existing -> {
                    rankingSnapshotHourlyRepository.delete(existing);
                    log.debug("Deleted existing snapshot for productId: {}, time: {}", 
                        productId, snapshotTime);
                });
            
            // 새 스냅샷 생성
            RankingSnapshotHourly snapshot = RankingSnapshotHourly.builder()
                .productId(productId)
                .totalScore(totalScore)
                .snapshotTime(snapshotTime)
                .build();
            
            rankingSnapshotHourlyRepository.save(snapshot);
            savedCount++;
        }
        
        log.info("Hourly snapshot created: {} products for time: {}", savedCount, snapshotTime);
    }

    /**
     * 일간 슬라이딩 윈도우 스냅샷 생성
     * 최근 24시간 데이터를 집계하여 Daily 스냅샷에 저장
     * 동적 weight를 적용하여 점수 계산
     * Fallback을 위한 확정 데이터 저장
     */
    @Transactional
    public void createDailySnapshot(LocalDateTime windowEnd) {
        LocalDateTime now = windowEnd;
        LocalDateTime twentyFourHoursAgo = now.minusHours(24);
        
        // 스냅샷 시간을 시간 단위로 정규화 (예: 10:00)
        LocalDateTime snapshotTime = now.withMinute(0).withSecond(0).withNano(0);
        
        log.info("Creating daily snapshot for time range: {} ~ {} (24-hour sliding window)", 
            twentyFourHoursAgo, now);
        
        // 최근 24시간 데이터를 이벤트 타입별로 집계
        List<Object[]> aggregates = rankingEventLogRepository
            .aggregateByProductIdAndEventTypeAndTimeRange(twentyFourHoursAgo, now);
        
        if (aggregates.isEmpty()) {
            log.debug("No events found in the last 24 hours, skipping snapshot creation");
            return;
        }
        
        // 상품별로 점수 집계 (동적 weight 적용)
        Map<Long, Double> productScores = new HashMap<>();
        for (Object[] aggregate : aggregates) {
            Long productId = (Long) aggregate[0];
            RankingEventType eventType = (RankingEventType) aggregate[1];
            Long eventCount = ((Number) aggregate[2]).longValue();
            BigDecimal sumRawPrice = aggregate[3] != null ? 
                (aggregate[3] instanceof BigDecimal ? (BigDecimal) aggregate[3] : 
                 BigDecimal.valueOf(((Number) aggregate[3]).doubleValue())) : BigDecimal.ZERO;
            Long sumRawQuantity = aggregate[4] != null ? 
                ((Number) aggregate[4]).longValue() : 0L;
            Double sumOrderScore = aggregate[5] != null ? 
                ((Number) aggregate[5]).doubleValue() : 0.0;
            
            if (productId == null || eventType == null) {
                continue;
            }
            
            // 동적 weight 조회
            double weight = rankingWeightService.getWeight(eventType);
            
            // 이벤트 타입별 점수 계산
            double score = calculateScoreByEventType(eventType, eventCount, sumRawPrice, sumRawQuantity, sumOrderScore, weight);
            
            productScores.merge(productId, score, Double::sum);
        }
        
        // 스냅샷 저장 (기존 스냅샷이 있으면 건너뛰고, 없으면 생성)
        // 슬라이딩 윈도우용 Daily Snapshot은 매 시간마다 갱신되므로,
        // 같은 snapshotTime에 대해 기존 스냅샷이 있으면 업데이트가 필요하지만,
        // 현재는 중복 방지를 위해 기존 스냅샷이 있으면 건너뜀
        // TODO: 추후 업데이트 로직 추가 필요 (Repository에 delete 메서드 추가 또는 엔티티에 update 메서드 추가)
        int savedCount = 0;
        int skippedCount = 0;
        for (Map.Entry<Long, Double> entry : productScores.entrySet()) {
            Long productId = entry.getKey();
            Double totalScore = entry.getValue();
            
            // 기존 스냅샷 확인
            var existing = rankingSnapshotDailyRepository
                .findByProductIdAndSnapshotTime(productId, snapshotTime);
            
            if (existing.isPresent()) {
                log.debug("Daily snapshot already exists for productId: {}, time: {}, skipping", 
                    productId, snapshotTime);
                skippedCount++;
                continue;
            }
            
            // 새 스냅샷 생성
            RankingSnapshotDaily snapshot = RankingSnapshotDaily.builder()
                .productId(productId)
                .totalScore(totalScore)
                .snapshotTime(snapshotTime)
                .build();
            
            rankingSnapshotDailyRepository.save(snapshot);
            savedCount++;
        }
        
        log.info("Daily snapshot created: {} products ({} new, {} skipped) for time: {} (24-hour sliding window)", 
            savedCount + skippedCount, savedCount, skippedCount, snapshotTime);
    }

    /**
     * 시간별 슬라이딩 윈도우 재구성 (최근 1시간 데이터)
     * DB의 Event Log를 집계하여 Redis의 ranking:hourly 키를 완전히 교체
     * Weight를 동적으로 적용하여 점수 계산
     * Atomic swap을 위해 임시 키를 사용한 후 RENAME으로 교체
     */
    @Transactional(readOnly = true)
    public void rebuildHourlyRanking() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.minusHours(1);
        
        log.info("Rebuilding hourly ranking (sliding window: last 1 hour from {})", oneHourAgo);
        
        try {
            // 1. DB에서 최근 1시간 데이터를 이벤트 타입별로 집계
            List<Object[]> aggregates = rankingEventLogRepository
                .aggregateByProductIdAndEventTypeAndTimeRange(oneHourAgo, now);
            
            if (aggregates.isEmpty()) {
                log.debug("No events found in the last hour, clearing hourly ranking");
                // 데이터가 없으면 기존 키 삭제
                redisTemplate.delete(RANKING_HOURLY_KEY);
                return;
            }
            
            // 2. 상품별로 점수 집계 (동적 weight 적용)
            Map<Long, Double> productScores = new HashMap<>();
            for (Object[] aggregate : aggregates) {
                Long productId = (Long) aggregate[0];
                RankingEventType eventType = (RankingEventType) aggregate[1];
                Long eventCount = ((Number) aggregate[2]).longValue();
                BigDecimal sumRawPrice = aggregate[3] != null ? 
                    (aggregate[3] instanceof BigDecimal ? (BigDecimal) aggregate[3] : 
                     BigDecimal.valueOf(((Number) aggregate[3]).doubleValue())) : BigDecimal.ZERO;
                Long sumRawQuantity = aggregate[4] != null ? 
                    ((Number) aggregate[4]).longValue() : 0L;
                Double sumOrderScore = aggregate[5] != null ? 
                    ((Number) aggregate[5]).doubleValue() : 0.0;
                
                if (productId == null || eventType == null) {
                    continue;
                }
                
                // 동적 weight 조회
                double weight = rankingWeightService.getWeight(eventType);
                
                // 이벤트 타입별 점수 계산
                double score = calculateScoreByEventType(eventType, eventCount, sumRawPrice, sumRawQuantity, sumOrderScore, weight);
                
                productScores.merge(productId, score, Double::sum);
            }
            
            // 3. 임시 키에 집계 결과 저장
            for (Map.Entry<Long, Double> entry : productScores.entrySet()) {
                redisTemplate.opsForZSet().add(
                    RANKING_HOURLY_TEMP_KEY, 
                    entry.getKey().toString(), 
                    entry.getValue()
                );
            }
            
            // 4. TTL 설정
            redisTemplate.expire(RANKING_HOURLY_TEMP_KEY, Duration.ofHours(2));
            
            // 5. Atomic swap: 임시 키를 본 키로 교체
            redisTemplate.rename(RANKING_HOURLY_TEMP_KEY, RANKING_HOURLY_KEY);
            
            log.info("Hourly ranking rebuilt: {} products", productScores.size());
        } catch (Exception e) {
            // 실패 시 임시 키 정리
            redisTemplate.delete(RANKING_HOURLY_TEMP_KEY);
            log.error("Failed to rebuild hourly ranking", e);
            throw e;
        }
    }

    /**
     * 일간 슬라이딩 윈도우 재구성 (최근 24시간 데이터)
     * DB의 Event Log를 집계하여 Redis의 ranking:daily 키를 완전히 교체
     * Weight를 동적으로 적용하여 점수 계산
     * Atomic swap을 위해 임시 키를 사용한 후 RENAME으로 교체
     */
    @Transactional(readOnly = true)
    public void rebuildDailyRanking() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime twentyFourHoursAgo = now.minusHours(24);
        
        log.info("Rebuilding daily ranking (sliding window: last 24 hours from {})", twentyFourHoursAgo);
        
        try {
            // 1. DB에서 최근 24시간 데이터를 이벤트 타입별로 집계
            List<Object[]> aggregates = rankingEventLogRepository
                .aggregateByProductIdAndEventTypeAndTimeRange(twentyFourHoursAgo, now);
            
            if (aggregates.isEmpty()) {
                log.debug("No events found in the last 24 hours, clearing daily ranking");
                // 데이터가 없으면 기존 키 삭제
                redisTemplate.delete(RANKING_DAILY_KEY);
                return;
            }
            
            // 2. 상품별로 점수 집계 (동적 weight 적용)
            Map<Long, Double> productScores = new HashMap<>();
            for (Object[] aggregate : aggregates) {
                Long productId = (Long) aggregate[0];
                RankingEventType eventType = (RankingEventType) aggregate[1];
                Long eventCount = ((Number) aggregate[2]).longValue();
                BigDecimal sumRawPrice = aggregate[3] != null ? 
                    (aggregate[3] instanceof BigDecimal ? (BigDecimal) aggregate[3] : 
                     BigDecimal.valueOf(((Number) aggregate[3]).doubleValue())) : BigDecimal.ZERO;
                Long sumRawQuantity = aggregate[4] != null ? 
                    ((Number) aggregate[4]).longValue() : 0L;
                Double sumOrderScore = aggregate[5] != null ? 
                    ((Number) aggregate[5]).doubleValue() : 0.0;
                
                if (productId == null || eventType == null) {
                    continue;
                }
                
                // 동적 weight 조회
                double weight = rankingWeightService.getWeight(eventType);
                
                // 이벤트 타입별 점수 계산
                double score = calculateScoreByEventType(eventType, eventCount, sumRawPrice, sumRawQuantity, sumOrderScore, weight);
                
                productScores.merge(productId, score, Double::sum);
            }
            
            // 3. 임시 키에 집계 결과 저장
            for (Map.Entry<Long, Double> entry : productScores.entrySet()) {
                redisTemplate.opsForZSet().add(
                    RANKING_DAILY_TEMP_KEY, 
                    entry.getKey().toString(), 
                    entry.getValue()
                );
            }
            
            // 4. TTL 설정
            redisTemplate.expire(RANKING_DAILY_TEMP_KEY, Duration.ofHours(25));
            
            // 5. Atomic swap: 임시 키를 본 키로 교체
            redisTemplate.rename(RANKING_DAILY_TEMP_KEY, RANKING_DAILY_KEY);
            
            log.info("Daily ranking rebuilt: {} products", productScores.size());
        } catch (Exception e) {
            // 실패 시 임시 키 정리
            redisTemplate.delete(RANKING_DAILY_TEMP_KEY);
            log.error("Failed to rebuild daily ranking", e);
            throw e;
        }
    }

    /**
     * 이벤트 타입별 점수 계산
     * ORDER: 각 주문별로 log10(price * quantity + 1)을 계산한 합계에 weight 곱하기
     * LIKE, VIEW: count * weight
     */
    private double calculateScoreByEventType(RankingEventType eventType, Long eventCount, 
                                            BigDecimal sumRawPrice, Long sumRawQuantity, 
                                            Double sumOrderScore, double weight) {
        return switch (eventType) {
            case ORDER -> {
                // ORDER의 경우: 쿼리에서 이미 각 주문별 log10(price * quantity + 1)의 합계를 계산했으므로
                // 그 값에 weight를 곱하면 됨
                yield (sumOrderScore != null ? sumOrderScore : 0.0) * weight;
            }
            case LIKE, VIEW -> {
                // LIKE, VIEW의 경우: 이벤트 발생 횟수 * weight
                yield eventCount * weight;
            }
        };
    }

}

