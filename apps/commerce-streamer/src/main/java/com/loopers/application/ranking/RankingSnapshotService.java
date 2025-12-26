package com.loopers.application.ranking;

import com.loopers.domain.ranking.RankingEventLogRepository;
import com.loopers.domain.ranking.RankingSnapshotHourly;
import com.loopers.domain.ranking.RankingSnapshotDaily;
import com.loopers.domain.ranking.RankingSnapshotHourlyRepository;
import com.loopers.domain.ranking.RankingSnapshotDailyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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

    private static final String RANKING_KEY_PREFIX = "ranking:all:";
    private static final String RANKING_HOURLY_KEY = "ranking:hourly";
    private static final String RANKING_DAILY_KEY = "ranking:daily";
    private static final String RANKING_HOURLY_TEMP_KEY = "ranking:hourly:temp";
    private static final String RANKING_DAILY_TEMP_KEY = "ranking:daily:temp";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 오늘 날짜 기반 랭킹 키 생성
     */
    private String getTodayKey() {
        String date = LocalDate.now().format(DATE_FORMATTER);
        return RANKING_KEY_PREFIX + date;
    }

    /**
     * 시간 단위 스냅샷 생성
     * 오늘 00:00부터 현재 시간까지의 이벤트 로그를 누적 집계하여 Hourly 스냅샷에 저장
     */
    @Transactional
    public void createHourlySnapshot(LocalDateTime windowEnd) {
        // windowEnd를 시간 단위로 정규화 (예: 10:05 → 10:00)
        LocalDateTime snapshotTime = windowEnd.withMinute(0).withSecond(0).withNano(0);
        // 오늘 00:00부터 현재까지 누적 집계
        LocalDateTime dayStart = snapshotTime.toLocalDate().atStartOfDay();
        
        log.info("Creating hourly snapshot for time range: {} ~ {} (cumulative)", dayStart, snapshotTime);
        
        // 오늘 00:00부터 현재까지의 이벤트 로그 누적 집계
        List<Object[]> aggregates = rankingEventLogRepository.aggregateByProductIdAndTimeRange(dayStart, snapshotTime);
        
        // 스냅샷 저장 (기존 스냅샷이 있으면 삭제 후 새로 생성)
        for (Object[] aggregate : aggregates) {
            Long productId = (Long) aggregate[0];
            Double totalScore = ((Number) aggregate[1]).doubleValue();
            
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
        }
        
        log.info("Hourly snapshot created: {} products for time: {}", aggregates.size(), snapshotTime);
    }

    /**
     * 스냅샷 데이터를 Redis에 동기화
     * 이전 시간대의 Hourly 스냅샷을 기반으로 Redis ZSET을 업데이트
     * 스냅샷은 오늘 00:00부터 누적 집계된 값이므로, Redis와 비교하여 보정
     */
    public void syncSnapshotToRedis() {
        LocalDateTime now = LocalDateTime.now();
        // 현재 시간을 정규화하고 이전 시간대 스냅샷 사용 (예: 10:05 → 9:00 스냅샷)
        LocalDateTime previousHour = now.withMinute(0).withSecond(0).withNano(0).minusHours(1);
        
        log.info("Syncing snapshot to Redis for time: {}", previousHour);
        
        // 이전 시간대 스냅샷 조회
        List<RankingSnapshotHourly> snapshots = rankingSnapshotHourlyRepository
            .findBySnapshotTimeOrderByTotalScoreDesc(previousHour);
        
        if (snapshots.isEmpty()) {
            log.warn("No snapshots found for time: {}", previousHour);
            return;
        }
        
        String todayKey = getTodayKey();
        
        // Redis ZSET에 스냅샷 데이터 반영
        // 스냅샷은 오늘 00:00부터 누적 집계된 값이므로, Redis 값과 차이가 있으면 스냅샷 값으로 보정
        for (RankingSnapshotHourly snapshot : snapshots) {
            Long productId = snapshot.getProductId();
            Double snapshotScore = snapshot.getTotalScore();
            
            if (productId == null || snapshotScore == null) {
                continue;
            }
            
            String productIdStr = productId.toString();
            
            // 현재 Redis 점수 조회
            Double currentScore = redisTemplate.opsForZSet().score(todayKey, productIdStr);
            
            // 스냅샷 점수가 더 크거나 같으면 스냅샷 값으로 업데이트
            // 스냅샷은 누적 집계이므로, Redis 값보다 크거나 같아야 정상
            // (오차 보정을 위해)
            if (currentScore == null || snapshotScore >= currentScore) {
                redisTemplate.opsForZSet().add(todayKey, productIdStr, snapshotScore);
            } else {
                // 스냅샷이 더 작으면 로그만 남기고 스킵 (실시간 이벤트가 더 최신일 수 있음)
                log.debug("Snapshot score ({}) is less than Redis score ({}) for productId: {}, skipping update", 
                    snapshotScore, currentScore, productId);
            }
        }
        
        // TTL 설정
        redisTemplate.expire(todayKey, Duration.ofDays(2));
        
        log.info("Snapshot synced to Redis: {} products", snapshots.size());
    }

    /**
     * 1시간 주기 스냅샷 집계 및 Redis 동기화
     */
    @Transactional
    public void createSnapshotAndSync() {
        LocalDateTime now = LocalDateTime.now();
        
        try {
            // 1. 스냅샷 생성 (오늘 00:00부터 현재까지 누적 집계)
            createHourlySnapshot(now);
            
            // 2. Redis 동기화 (방금 생성한 스냅샷 반영)
            syncSnapshotToRedis();
            
            log.info("Snapshot creation and sync completed");
        } catch (Exception e) {
            log.error("Failed to create snapshot and sync", e);
            throw e;
        }
    }

    /**
     * Daily 스냅샷 생성 (Hourly 스냅샷에서 최종 값 추출)
     * 어제의 마지막 Hourly 스냅샷(23:00)을 사용하여 Daily 스냅샷 생성
     * Hourly 스냅샷은 누적 점수이므로, 가장 마지막 시간대의 스냅샷만 사용하면 됨
     */
    @Transactional
    public void createDailySnapshotFromHourly(LocalDate targetDate) {
        LocalDateTime dayStart = targetDate.atStartOfDay();
        // 해당 날짜의 마지막 시간대 스냅샷 (23:00) 사용
        LocalDateTime lastHourOfDay = targetDate.atTime(23, 0, 0);
        
        log.info("Creating daily snapshot from hourly snapshots for date: {} (using snapshot at {})", 
            targetDate, lastHourOfDay);
        
        // 해당 날짜의 마지막 시간대 스냅샷 조회
        List<RankingSnapshotHourly> hourlySnapshots = rankingSnapshotHourlyRepository
            .findBySnapshotTimeOrderByTotalScoreDesc(lastHourOfDay);
        
        if (hourlySnapshots.isEmpty()) {
            log.warn("No hourly snapshot found for date: {} at time: {}", targetDate, lastHourOfDay);
            // 마지막 시간대 스냅샷이 없으면, 해당 날짜의 가장 최신 스냅샷 찾기
            List<RankingSnapshotHourly> allDaySnapshots = rankingSnapshotHourlyRepository
                .findBySnapshotTimeBetween(dayStart, targetDate.atTime(23, 59, 59));
            
            if (allDaySnapshots.isEmpty()) {
                log.warn("No hourly snapshots found for date: {}", targetDate);
                return;
            }
            
            // 각 상품별로 가장 최신 스냅샷 찾기
            Map<Long, RankingSnapshotHourly> latestSnapshots = new HashMap<>();
            for (RankingSnapshotHourly snapshot : allDaySnapshots) {
                Long productId = snapshot.getProductId();
                RankingSnapshotHourly existing = latestSnapshots.get(productId);
                
                if (existing == null || snapshot.getSnapshotTime().isAfter(existing.getSnapshotTime())) {
                    latestSnapshots.put(productId, snapshot);
                }
            }
            
            hourlySnapshots = List.copyOf(latestSnapshots.values());
            log.info("Using latest snapshots per product for date: {}, found {} products", 
                targetDate, hourlySnapshots.size());
        }
        
        // Daily 스냅샷 저장 (날짜의 00:00:00으로 정규화)
        LocalDateTime snapshotTime = dayStart;
        int savedCount = 0;
        for (RankingSnapshotHourly hourly : hourlySnapshots) {
            Long productId = hourly.getProductId();
            Double totalScore = hourly.getTotalScore();
            
            if (productId == null || totalScore == null) {
                continue;
            }
            
            // 기존 스냅샷이 있으면 업데이트, 없으면 생성
            RankingSnapshotDaily existing = rankingSnapshotDailyRepository
                .findByProductIdAndSnapshotTime(productId, snapshotTime)
                .orElse(null);
            
            if (existing != null) {
                log.debug("Daily snapshot already exists for productId: {}, date: {}", productId, targetDate);
            } else {
                RankingSnapshotDaily snapshot = RankingSnapshotDaily.builder()
                    .productId(productId)
                    .totalScore(totalScore)
                    .snapshotTime(snapshotTime)
                    .build();
                
                rankingSnapshotDailyRepository.save(snapshot);
                savedCount++;
            }
        }
        
        log.info("Daily snapshot created: {} products for date: {}", savedCount, targetDate);
    }

    /**
     * 시간별 슬라이딩 윈도우 재구성 (최근 1시간 데이터)
     * DB의 Event Log를 집계하여 Redis의 ranking:hourly 키를 완전히 교체
     * Atomic swap을 위해 임시 키를 사용한 후 RENAME으로 교체
     */
    @Transactional(readOnly = true)
    public void rebuildHourlyRanking() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.minusHours(1);
        
        log.info("Rebuilding hourly ranking (sliding window: last 1 hour from {})", oneHourAgo);
        
        try {
            // 1. DB에서 최근 1시간 데이터 집계
            List<Object[]> aggregates = rankingEventLogRepository
                .aggregateByProductIdAndTimeRange(oneHourAgo, now);
            
            if (aggregates.isEmpty()) {
                log.debug("No events found in the last hour, clearing hourly ranking");
                // 데이터가 없으면 기존 키 삭제
                redisTemplate.delete(RANKING_HOURLY_KEY);
                return;
            }
            
            // 2. 임시 키에 집계 결과 저장
            for (Object[] aggregate : aggregates) {
                Long productId = (Long) aggregate[0];
                Double totalScore = ((Number) aggregate[1]).doubleValue();
                
                if (productId == null || totalScore == null) {
                    continue;
                }
                
                redisTemplate.opsForZSet().add(
                    RANKING_HOURLY_TEMP_KEY, 
                    productId.toString(), 
                    totalScore
                );
            }
            
            // 3. TTL 설정
            redisTemplate.expire(RANKING_HOURLY_TEMP_KEY, Duration.ofHours(2));
            
            // 4. Atomic swap: 임시 키를 본 키로 교체
            redisTemplate.rename(RANKING_HOURLY_TEMP_KEY, RANKING_HOURLY_KEY);
            
            log.info("Hourly ranking rebuilt: {} products", aggregates.size());
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
     * Atomic swap을 위해 임시 키를 사용한 후 RENAME으로 교체
     */
    @Transactional(readOnly = true)
    public void rebuildDailyRanking() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime twentyFourHoursAgo = now.minusHours(24);
        
        log.info("Rebuilding daily ranking (sliding window: last 24 hours from {})", twentyFourHoursAgo);
        
        try {
            // 1. DB에서 최근 24시간 데이터 집계
            List<Object[]> aggregates = rankingEventLogRepository
                .aggregateByProductIdAndTimeRange(twentyFourHoursAgo, now);
            
            if (aggregates.isEmpty()) {
                log.debug("No events found in the last 24 hours, clearing daily ranking");
                // 데이터가 없으면 기존 키 삭제
                redisTemplate.delete(RANKING_DAILY_KEY);
                return;
            }
            
            // 2. 임시 키에 집계 결과 저장
            for (Object[] aggregate : aggregates) {
                Long productId = (Long) aggregate[0];
                Double totalScore = ((Number) aggregate[1]).doubleValue();
                
                if (productId == null || totalScore == null) {
                    continue;
                }
                
                redisTemplate.opsForZSet().add(
                    RANKING_DAILY_TEMP_KEY, 
                    productId.toString(), 
                    totalScore
                );
            }
            
            // 3. TTL 설정
            redisTemplate.expire(RANKING_DAILY_TEMP_KEY, Duration.ofHours(25));
            
            // 4. Atomic swap: 임시 키를 본 키로 교체
            redisTemplate.rename(RANKING_DAILY_TEMP_KEY, RANKING_DAILY_KEY);
            
            log.info("Daily ranking rebuilt: {} products", aggregates.size());
        } catch (Exception e) {
            // 실패 시 임시 키 정리
            redisTemplate.delete(RANKING_DAILY_TEMP_KEY);
            log.error("Failed to rebuild daily ranking", e);
            throw e;
        }
    }

}

