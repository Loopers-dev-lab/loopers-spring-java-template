package com.loopers.application.ranking;

import com.loopers.domain.ranking.RankingEventLog;
import com.loopers.domain.ranking.RankingEventLogRepository;
import com.loopers.domain.ranking.RankingEventType;
import com.loopers.domain.ranking.RankingSnapshotHourly;
import com.loopers.domain.ranking.RankingSnapshotHourlyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 랭킹 복구 서비스
 * 스냅샷 기반으로 Redis 데이터를 복구
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RankingRecoveryService {

    private final RankingSnapshotHourlyRepository rankingSnapshotHourlyRepository;
    private final RankingEventLogRepository rankingEventLogRepository;
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
     * 스냅샷 기반 Redis 데이터 복구
     * 1. 최신 스냅샷을 Redis로 로드
     * 2. 스냅샷 시점 이후의 이벤트 로그를 합산하여 Redis에 추가 반영
     */
    @Transactional(readOnly = true)
    public void recoverFromSnapshot() {
        log.info("Starting ranking recovery from snapshot...");

        try {
            // 1. 최신 Hourly 스냅샷 조회
            RankingSnapshotHourly latestSnapshot = rankingSnapshotHourlyRepository
                .findTopByOrderBySnapshotTimeDesc()
                .orElse(null);

            if (latestSnapshot == null) {
                log.warn("No snapshot found for recovery");
                return;
            }

            LocalDateTime snapshotTime = latestSnapshot.getSnapshotTime();
            log.info("Latest snapshot found: hour={}, {} products", 
                snapshotTime, rankingSnapshotHourlyRepository.count());

            // 2. 스냅샷 데이터를 Redis로 로드
            restoreRedisFromSnapshot(latestSnapshot);

            // 3. 스냅샷 시점 이후의 이벤트 로그들을 합산하여 추가 반영
            catchUpWithEventLogs(snapshotTime);

            log.info("Ranking recovery completed successfully");
        } catch (Exception e) {
            log.error("Failed to recover ranking from snapshot", e);
            throw e;
        }
    }

    /**
     * 스냅샷 데이터를 Redis로 복원
     */
    private void restoreRedisFromSnapshot(RankingSnapshotHourly snapshot) {
        String todayKey = getTodayKey();
        LocalDateTime snapshotTime = snapshot.getSnapshotTime();

        // 해당 시간대의 모든 스냅샷 조회
        List<RankingSnapshotHourly> snapshots = rankingSnapshotHourlyRepository
            .findBySnapshotTimeOrderByTotalScoreDesc(snapshotTime);

        log.info("Restoring {} products from snapshot (time: {})", snapshots.size(), snapshotTime);

        // Redis에 스냅샷 데이터 저장
        for (RankingSnapshotHourly snapshotItem : snapshots) {
            Long productId = snapshotItem.getProductId();
            Double totalScore = snapshotItem.getTotalScore();

            if (productId == null || totalScore == null) {
                continue;
            }

            String productIdStr = productId.toString();
            redisTemplate.opsForZSet().add(todayKey, productIdStr, totalScore);
        }

        // TTL 설정
        redisTemplate.expire(todayKey, Duration.ofDays(2));

        log.info("Redis restored from snapshot: {} products", snapshots.size());
    }

    /**
     * 스냅샷 시점 이후의 이벤트 로그들을 합산하여 Redis에 추가 반영 (Catch-up)
     */
    private void catchUpWithEventLogs(LocalDateTime snapshotTime) {
        log.info("Catching up with event logs after snapshot time: {}", snapshotTime);

        // 스냅샷 시점 이후의 이벤트 로그 조회
        List<RankingEventLog> eventLogs = rankingEventLogRepository.findByOccurredAtAfter(snapshotTime);

        if (eventLogs.isEmpty()) {
            log.info("No event logs found after snapshot time");
            return;
        }

        log.info("Applying {} event logs to Redis", eventLogs.size());

        // 각 이벤트 로그를 Redis에 반영
        for (RankingEventLog eventLog : eventLogs) {
            Long productId = eventLog.getProductId();
            Double score = eventLog.getScore();
            RankingEventType eventType = eventLog.getEventType();

            if (productId == null || score == null) {
                continue;
            }

            try {
                String todayKey = getTodayKey();
                String productIdStr = productId.toString();

                // Redis ZSET에 점수 추가
                redisTemplate.opsForZSet().incrementScore(todayKey, productIdStr, score);
                redisTemplate.expire(todayKey, Duration.ofDays(2));

                log.debug("Applied event log: productId={}, type={}, score={}", 
                    productId, eventType, score);
            } catch (Exception e) {
                log.error("Failed to apply event log to Redis: productId={}, error={}", 
                    productId, e.getMessage(), e);
                // 개별 실패는 로깅만 하고 계속 진행
            }
        }

        log.info("Event logs catch-up completed: {} logs applied", eventLogs.size());
    }
}

