package com.loopers.batch.ranking.step3;

import com.loopers.batch.ranking.RankingType;
import com.loopers.domain.ranking.RankingSnapshotHourly;
import com.loopers.domain.ranking.RankingSnapshotDaily;
import com.loopers.domain.ranking.RankingSnapshotWeekly;
import com.loopers.domain.ranking.RankingSnapshotMonthly;
import com.loopers.infrastructure.ranking.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

/**
 * Step 3-1: Tier 1 Redis ZSET 동기화 Tasklet (랭킹 타입별)
 * 
 * RankingSnapshotX에서 최신 스냅샷을 조회하여 Redis ZSET에 동기화
 * 원자적 교체를 위해 임시 키 사용 후 RENAME
 */
@Slf4j
public class Tier1SyncTasklet implements Tasklet {

    private final RankingType rankingType;
    private final StringRedisTemplate redisTemplate;
    private final RankingSnapshotHourlyJpaRepository hourlySnapshotRepository;
    private final RankingSnapshotDailyJpaRepository dailySnapshotRepository;
    private final RankingSnapshotWeeklyJpaRepository weeklySnapshotRepository;
    private final RankingSnapshotMonthlyJpaRepository monthlySnapshotRepository;

    private static final int SNAPSHOT_LIMIT = 500;
    private static final long TEMP_KEY_TTL_SECONDS = 300; // 5분

    public Tier1SyncTasklet(RankingType rankingType,
                             StringRedisTemplate redisTemplate,
                             RankingSnapshotHourlyJpaRepository hourlySnapshotRepository,
                             RankingSnapshotDailyJpaRepository dailySnapshotRepository,
                             RankingSnapshotWeeklyJpaRepository weeklySnapshotRepository,
                             RankingSnapshotMonthlyJpaRepository monthlySnapshotRepository) {
        this.rankingType = rankingType;
        this.redisTemplate = redisTemplate;
        this.hourlySnapshotRepository = hourlySnapshotRepository;
        this.dailySnapshotRepository = dailySnapshotRepository;
        this.weeklySnapshotRepository = weeklySnapshotRepository;
        this.monthlySnapshotRepository = monthlySnapshotRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        log.info("Tier1SyncTasklet 시작: type={}", rankingType);

        try {
            // 1. Redis 연결 확인
            if (!isRedisAvailable()) {
                log.warn("Redis 연결 실패. Step을 SKIP합니다.");
                contribution.setExitStatus(ExitStatus.COMPLETED);
                return RepeatStatus.FINISHED;
            }

            // 2. 최신 스냅샷 조회 (상위 500개)
            List<?> snapshots = getLatestSnapshots();
            
            if (snapshots.isEmpty()) {
                log.debug("{} 스냅샷이 없습니다. Step을 SKIP합니다.", rankingType);
                return RepeatStatus.FINISHED;
            }

            // 3. Redis 키 설정
            String tempKey = getTempKey();
            String finalKey = getFinalKey();

            // 4. 임시 ZSET에 데이터 저장
            redisTemplate.delete(tempKey); // 기존 임시 키 삭제 (안전)
            
            for (Object snapshot : snapshots) {
                Long productId = getProductId(snapshot);
                Double totalScore = getTotalScore(snapshot);
                
                if (productId != null && totalScore != null) {
                    redisTemplate.opsForZSet().add(tempKey, productId.toString(), totalScore);
                }
            }

            // 5. TTL 설정 (5분)
            redisTemplate.expire(tempKey, Duration.ofSeconds(TEMP_KEY_TTL_SECONDS));

            // 6. 원자적 교체: RENAME
            redisTemplate.rename(tempKey, finalKey);

            log.info("Tier1SyncTasklet 완료: type={}, synced={}건", rankingType, snapshots.size());
            return RepeatStatus.FINISHED;

        } catch (Exception e) {
            log.error("Tier1SyncTasklet 실패: type={}", rankingType, e);
            // Redis 장애 시 Step을 SKIP하고 배치는 성공 상태로 완료
            contribution.setExitStatus(ExitStatus.COMPLETED);
            return RepeatStatus.FINISHED;
        }
    }

    /**
     * 최신 스냅샷 조회 (상위 500개)
     */
    private List<?> getLatestSnapshots() {
        List<?> snapshots = switch (rankingType) {
            case HOURLY -> hourlySnapshotRepository.findLatestSnapshotOrderByRank();
            case DAILY -> dailySnapshotRepository.findLatestSnapshotOrderByRank();
            case WEEKLY -> weeklySnapshotRepository.findLatestSnapshotOrderByRank();
            case MONTHLY -> monthlySnapshotRepository.findLatestSnapshotOrderByRank();
        };
        return snapshots.stream().limit(SNAPSHOT_LIMIT).toList();
    }

    /**
     * Redis 임시 키
     */
    private String getTempKey() {
        return String.format("ranking:%s:temp", rankingType.name().toLowerCase());
    }

    /**
     * Redis 최종 키
     */
    private String getFinalKey() {
        return String.format("ranking:%s", rankingType.name().toLowerCase());
    }

    /**
     * ProductId 추출
     */
    private Long getProductId(Object snapshot) {
        return switch (snapshot) {
            case RankingSnapshotHourly h -> h.getProductId();
            case RankingSnapshotDaily d -> d.getProductId();
            case RankingSnapshotWeekly w -> w.getProductId();
            case RankingSnapshotMonthly m -> m.getProductId();
            default -> throw new IllegalArgumentException("Unknown snapshot type: " + snapshot.getClass());
        };
    }

    /**
     * TotalScore 추출
     */
    private Double getTotalScore(Object snapshot) {
        return switch (snapshot) {
            case RankingSnapshotHourly h -> h.getTotalScore();
            case RankingSnapshotDaily d -> d.getTotalScore();
            case RankingSnapshotWeekly w -> w.getTotalScore();
            case RankingSnapshotMonthly m -> m.getTotalScore();
            default -> throw new IllegalArgumentException("Unknown snapshot type: " + snapshot.getClass());
        };
    }

    /**
     * Redis 연결 상태 확인
     */
    private boolean isRedisAvailable() {
        try {
            redisTemplate.hasKey("ping");
            return true;
        } catch (Exception e) {
            log.debug("Redis 연결 확인 실패", e);
            return false;
        }
    }
}

