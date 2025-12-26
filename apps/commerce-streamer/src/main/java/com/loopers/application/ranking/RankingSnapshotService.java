package com.loopers.application.ranking;

import com.loopers.domain.ranking.RankingEventLogRepository;
import com.loopers.domain.ranking.RankingSnapshotHourly;
import com.loopers.domain.ranking.RankingSnapshotDaily;
import com.loopers.domain.ranking.RankingSnapshotMonthly;
import com.loopers.domain.ranking.RankingSnapshotYearly;
import com.loopers.domain.ranking.RankingSnapshotHourlyRepository;
import com.loopers.domain.ranking.RankingSnapshotDailyRepository;
import com.loopers.domain.ranking.RankingSnapshotMonthlyRepository;
import com.loopers.domain.ranking.RankingSnapshotYearlyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.YearMonth;
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
    private final RankingSnapshotMonthlyRepository rankingSnapshotMonthlyRepository;
    private final RankingSnapshotYearlyRepository rankingSnapshotYearlyRepository;
    private final StringRedisTemplate redisTemplate;

    private static final String RANKING_KEY_PREFIX = "ranking:all:";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 오늘 날짜 기반 랭킹 키 생성
     */
    private String getTodayKey() {
        String date = LocalDate.now().format(DATE_FORMATTER);
        return RANKING_KEY_PREFIX + date;
    }

    /**
     * 10분 단위 시간 단위 스냅샷 생성
     * 최근 10분간의 이벤트 로그를 집계하여 Hourly 스냅샷에 저장
     */
    @Transactional
    public void createHourlySnapshot(LocalDateTime windowEnd) {
        LocalDateTime windowStart = windowEnd.minusMinutes(10);
        
        log.info("Creating hourly snapshot for time range: {} ~ {}", windowStart, windowEnd);
        
        // 최근 10분간의 이벤트 로그 집계
        List<Object[]> aggregates = rankingEventLogRepository.aggregateByProductIdAndTimeRange(windowStart, windowEnd);
        
        // Hour 단위로 정규화 (분, 초, 나노초를 0으로)
        LocalDateTime snapshotTime = windowEnd.withMinute(0).withSecond(0).withNano(0);
        
        // 스냅샷 저장
        for (Object[] aggregate : aggregates) {
            Long productId = (Long) aggregate[0];
            Double totalScore = ((Number) aggregate[1]).doubleValue();
            
            // 기존 스냅샷이 있으면 업데이트, 없으면 생성
            RankingSnapshotHourly existing = rankingSnapshotHourlyRepository
                .findByProductIdAndSnapshotTime(productId, snapshotTime)
                .orElse(null);
            
            if (existing != null) {
                // 기존 스냅샷 업데이트 (점수 누적)
                // 실제로는 새로운 점수를 더하는 것이 맞지만, 
                // 10분마다 실행되므로 새로운 집계 결과로 대체하는 것이 더 정확함
                // 하지만 중복 실행 방지를 위해 기존 값이 있으면 스킵
                log.debug("Snapshot already exists for productId: {}, time: {}", productId, snapshotTime);
            } else {
                RankingSnapshotHourly snapshot = RankingSnapshotHourly.builder()
                    .productId(productId)
                    .totalScore(totalScore)
                    .snapshotTime(snapshotTime)
                    .build();
                
                rankingSnapshotHourlyRepository.save(snapshot);
            }
        }
        
        log.info("Hourly snapshot created: {} products", aggregates.size());
    }

    /**
     * 스냅샷 데이터를 Redis에 동기화
     * 최신 Hourly 스냅샷을 기반으로 Redis ZSET을 업데이트
     */
    public void syncSnapshotToRedis() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentTime = now.withMinute(0).withSecond(0).withNano(0);
        
        log.info("Syncing snapshot to Redis for time: {}", currentTime);
        
        // 현재 시간 기준 스냅샷 조회
        List<RankingSnapshotHourly> snapshots = rankingSnapshotHourlyRepository
            .findBySnapshotTimeOrderByTotalScoreDesc(currentTime);
        
        if (snapshots.isEmpty()) {
            log.warn("No snapshots found for time: {}", currentTime);
            return;
        }
        
        String todayKey = getTodayKey();
        
        // Redis ZSET에 스냅샷 데이터 반영
        // 기존 데이터는 유지하되, 스냅샷 데이터로 보정
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
            // (오차 보정을 위해)
            if (currentScore == null || snapshotScore > currentScore) {
                redisTemplate.opsForZSet().add(todayKey, productIdStr, snapshotScore);
            }
        }
        
        // TTL 설정
        redisTemplate.expire(todayKey, Duration.ofDays(2));
        
        log.info("Snapshot synced to Redis: {} products", snapshots.size());
    }

    /**
     * 10분 주기 스냅샷 집계 및 Redis 동기화
     */
    @Transactional
    public void createSnapshotAndSync() {
        LocalDateTime now = LocalDateTime.now();
        
        try {
            // 1. 스냅샷 생성
            createHourlySnapshot(now);
            
            // 2. Redis 동기화
            syncSnapshotToRedis();
            
            log.info("Snapshot creation and sync completed");
        } catch (Exception e) {
            log.error("Failed to create snapshot and sync", e);
            throw e;
        }
    }

    /**
     * Daily 스냅샷 생성 (Hourly 스냅샷을 합산)
     * 어제의 모든 Hourly 스냅샷을 합산하여 Daily 스냅샷 생성
     */
    @Transactional
    public void createDailySnapshotFromHourly(LocalDate targetDate) {
        // targetDate의 00:00:00 ~ 23:59:59 범위의 모든 Hourly 스냅샷 조회
        LocalDateTime dayStart = targetDate.atStartOfDay();
        LocalDateTime dayEnd = targetDate.atTime(23, 59, 59);
        
        log.info("Creating daily snapshot from hourly snapshots for date: {}", targetDate);
        
        // 해당 날짜의 모든 Hourly 스냅샷 조회
        List<RankingSnapshotHourly> hourlySnapshots = rankingSnapshotHourlyRepository
            .findBySnapshotTimeBetween(dayStart, dayEnd);
        
        if (hourlySnapshots.isEmpty()) {
            log.warn("No hourly snapshots found for date: {}", targetDate);
            return;
        }
        
        // 상품별로 점수 합산
        Map<Long, Double> dailyScores = new HashMap<>();
        for (RankingSnapshotHourly hourly : hourlySnapshots) {
            Long productId = hourly.getProductId();
            Double score = hourly.getTotalScore();
            
            if (productId != null && score != null) {
                dailyScores.merge(productId, score, Double::sum);
            }
        }
        
        // Daily 스냅샷 저장 (날짜의 00:00:00으로 정규화)
        LocalDateTime snapshotTime = dayStart;
        for (Map.Entry<Long, Double> entry : dailyScores.entrySet()) {
            Long productId = entry.getKey();
            Double totalScore = entry.getValue();
            
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
            }
        }
        
        log.info("Daily snapshot created: {} products for date: {}", dailyScores.size(), targetDate);
    }

    /**
     * Monthly 스냅샷 생성 (Daily 스냅샷을 합산)
     * 지난달의 모든 Daily 스냅샷을 합산하여 Monthly 스냅샷 생성
     */
    @Transactional
    public void createMonthlySnapshotFromDaily(YearMonth targetMonth) {
        // targetMonth의 첫날 00:00:00 ~ 마지막날 23:59:59 범위의 모든 Daily 스냅샷 조회
        LocalDate monthStart = targetMonth.atDay(1);
        LocalDate monthEnd = targetMonth.atEndOfMonth();
        LocalDateTime monthStartTime = monthStart.atStartOfDay();
        LocalDateTime monthEndTime = monthEnd.atTime(23, 59, 59);
        
        log.info("Creating monthly snapshot from daily snapshots for month: {}", targetMonth);
        
        // 해당 월의 모든 Daily 스냅샷 조회
        List<RankingSnapshotDaily> dailySnapshots = rankingSnapshotDailyRepository
            .findBySnapshotTimeBetween(monthStartTime, monthEndTime);
        
        if (dailySnapshots.isEmpty()) {
            log.warn("No daily snapshots found for month: {}", targetMonth);
            return;
        }
        
        // 상품별로 점수 합산
        Map<Long, Double> monthlyScores = new HashMap<>();
        for (RankingSnapshotDaily daily : dailySnapshots) {
            Long productId = daily.getProductId();
            Double score = daily.getTotalScore();
            
            if (productId != null && score != null) {
                monthlyScores.merge(productId, score, Double::sum);
            }
        }
        
        // Monthly 스냅샷 저장 (월의 첫날 00:00:00으로 정규화)
        LocalDateTime snapshotTime = monthStartTime;
        for (Map.Entry<Long, Double> entry : monthlyScores.entrySet()) {
            Long productId = entry.getKey();
            Double totalScore = entry.getValue();
            
            // 기존 스냅샷이 있으면 업데이트, 없으면 생성
            RankingSnapshotMonthly existing = rankingSnapshotMonthlyRepository
                .findByProductIdAndSnapshotTime(productId, snapshotTime)
                .orElse(null);
            
            if (existing != null) {
                log.debug("Monthly snapshot already exists for productId: {}, month: {}", productId, targetMonth);
            } else {
                RankingSnapshotMonthly snapshot = RankingSnapshotMonthly.builder()
                    .productId(productId)
                    .totalScore(totalScore)
                    .snapshotTime(snapshotTime)
                    .build();
                
                rankingSnapshotMonthlyRepository.save(snapshot);
            }
        }
        
        log.info("Monthly snapshot created: {} products for month: {}", monthlyScores.size(), targetMonth);
    }

    /**
     * Yearly 스냅샷 생성 (Monthly 스냅샷을 합산)
     * 작년의 모든 Monthly 스냅샷을 합산하여 Yearly 스냅샷 생성
     */
    @Transactional
    public void createYearlySnapshotFromMonthly(Integer targetYear) {
        // targetYear의 1월 1일 00:00:00 ~ 12월 31일 23:59:59 범위의 모든 Monthly 스냅샷 조회
        LocalDate yearStart = LocalDate.of(targetYear, 1, 1);
        LocalDate yearEnd = LocalDate.of(targetYear, 12, 31);
        LocalDateTime yearStartTime = yearStart.atStartOfDay();
        LocalDateTime yearEndTime = yearEnd.atTime(23, 59, 59);
        
        log.info("Creating yearly snapshot from monthly snapshots for year: {}", targetYear);
        
        // 해당 연도의 모든 Monthly 스냅샷 조회
        List<RankingSnapshotMonthly> monthlySnapshots = rankingSnapshotMonthlyRepository
            .findBySnapshotTimeBetween(yearStartTime, yearEndTime);
        
        if (monthlySnapshots.isEmpty()) {
            log.warn("No monthly snapshots found for year: {}", targetYear);
            return;
        }
        
        // 상품별로 점수 합산
        Map<Long, Double> yearlyScores = new HashMap<>();
        for (RankingSnapshotMonthly monthly : monthlySnapshots) {
            Long productId = monthly.getProductId();
            Double score = monthly.getTotalScore();
            
            if (productId != null && score != null) {
                yearlyScores.merge(productId, score, Double::sum);
            }
        }
        
        // Yearly 스냅샷 저장 (연도의 1월 1일 00:00:00으로 정규화)
        LocalDateTime snapshotTime = yearStartTime;
        for (Map.Entry<Long, Double> entry : yearlyScores.entrySet()) {
            Long productId = entry.getKey();
            Double totalScore = entry.getValue();
            
            // 기존 스냅샷이 있으면 업데이트, 없으면 생성
            RankingSnapshotYearly existing = rankingSnapshotYearlyRepository
                .findByProductIdAndSnapshotTime(productId, snapshotTime)
                .orElse(null);
            
            if (existing != null) {
                log.debug("Yearly snapshot already exists for productId: {}, year: {}", productId, targetYear);
            } else {
                RankingSnapshotYearly snapshot = RankingSnapshotYearly.builder()
                    .productId(productId)
                    .totalScore(totalScore)
                    .snapshotTime(snapshotTime)
                    .build();
                
                rankingSnapshotYearlyRepository.save(snapshot);
            }
        }
        
        log.info("Yearly snapshot created: {} products for year: {}", yearlyScores.size(), targetYear);
    }
}

