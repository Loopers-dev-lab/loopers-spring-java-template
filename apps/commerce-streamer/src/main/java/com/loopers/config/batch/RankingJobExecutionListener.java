package com.loopers.config.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 랭킹 Job 실행 리스너
 * 분산 락 관리 및 Redis 장애 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RankingJobExecutionListener implements JobExecutionListener {

    private final RedissonClient redissonClient;
    private static final String FULL_RESYNC_LOCK_KEY = "ranking:full-resync:lock";
    private static final String INCREMENTAL_UPDATE_LOCK_KEY = "ranking:incremental-update:lock";
    private static final long INCREMENTAL_LOCK_TTL_SECONDS = 600; // 10분
    private static final long FULL_RESYNC_LOCK_TTL_SECONDS = 1800; // 30분
    private static final long LOCK_WAIT_TIME_SECONDS = 1; // 1초 대기
    private static final long HEARTBEAT_INTERVAL_SECONDS = 300; // 5분마다 Heartbeat
    private static final long MAX_JOB_EXECUTION_SECONDS = 3600; // 최대 1시간 (무한 락 유지 방지)

    private String lockValue;
    private RLock incrementalLock;
    private RLock fullResyncLock;
    private ScheduledExecutorService heartbeatExecutor;
    private long heartbeatStartTime;

    @Override
    public void beforeJob(@NonNull JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();
        log.info("RankingJob 시작 전 락 체크: jobName={}, jobInstanceId={}", 
            jobName, jobExecution.getJobInstance().getInstanceId());

        try {
            // 1. Redis 연결 확인
            if (!isRedisAvailable()) {
                log.warn("Redis 연결 실패. 락 체크를 스킵하고 Job 실행 (가용성 우선)");
                jobExecution.getExecutionContext().put("lockSkipped", true);
                return;
            }

            // 2. Job 타입에 따라 다른 락 전략 적용
            if ("rankingFullResyncJob".equals(jobName)) {
                handleFullResyncJob(jobExecution);
            } else if ("productRankingUpdateJob".equals(jobName)) {
                handleIncrementalUpdateJob(jobExecution);
            } else {
                log.warn("알 수 없는 Job 이름: {}. 락 체크를 스킵합니다.", jobName);
                jobExecution.getExecutionContext().put("lockSkipped", true);
            }

        } catch (Exception e) {
            log.error("락 체크 중 오류 발생. 락 체크를 스킵하고 Job 실행 (가용성 우선)", e);
            jobExecution.getExecutionContext().put("lockSkipped", true);
            // 예외를 던지지 않고 Job 실행 계속
        }
    }

    /**
     * Full Re-sync Job 락 처리
     */
    private void handleFullResyncJob(JobExecution jobExecution) {
        lockValue = UUID.randomUUID().toString();
        fullResyncLock = redissonClient.getLock(FULL_RESYNC_LOCK_KEY);
        
        try {
            boolean acquired = fullResyncLock.tryLock(LOCK_WAIT_TIME_SECONDS, FULL_RESYNC_LOCK_TTL_SECONDS, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("Full Re-sync 락 획득 실패. 이미 다른 인스턴스가 실행 중입니다. Job을 스킵합니다.");
                stopJob(jobExecution);
                return;
            }

            log.info("Full Re-sync 락 획득 성공: lockValue={}, TTL={}초", lockValue, FULL_RESYNC_LOCK_TTL_SECONDS);
            setLockContext(jobExecution, "fullResync");

            // Heartbeat 시작 (JobExecution 전달하여 최대 실행 시간 초과 시 Job 중단 가능)
            startHeartbeat(fullResyncLock, jobExecution);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Full Re-sync 락 획득 중 인터럽트 발생", e);
            stopJob(jobExecution);
        }
    }

    /**
     * 증분 업데이트 Job 락 처리
     */
    private void handleIncrementalUpdateJob(JobExecution jobExecution) {
        // 1. Full Re-sync 락 확인
        RLock fullResyncLock = redissonClient.getLock(FULL_RESYNC_LOCK_KEY);
        if (fullResyncLock.isLocked()) {
            log.warn("Full Re-sync가 실행 중입니다. 증분 업데이트 Job을 스킵합니다.");
            stopJob(jobExecution);
            return;
        }

        // 2. Incremental Update 락 획득 시도
        lockValue = UUID.randomUUID().toString();
        incrementalLock = redissonClient.getLock(INCREMENTAL_UPDATE_LOCK_KEY);
        
        try {
            boolean acquired = incrementalLock.tryLock(LOCK_WAIT_TIME_SECONDS, INCREMENTAL_LOCK_TTL_SECONDS, TimeUnit.SECONDS);
            if (!acquired) {
                log.warn("다른 인스턴스가 이미 실행 중입니다. Job을 스킵합니다.");
                stopJob(jobExecution);
                return;
            }

            log.info("Incremental Update 락 획득 성공: lockValue={}", lockValue);
            setLockContext(jobExecution, "incremental");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Incremental Update 락 획득 중 인터럽트 발생", e);
            stopJob(jobExecution);
        }
    }

    /**
     * Heartbeat 시작 (Full Re-sync Job 전용)
     * 5분마다 TTL 갱신, 최대 1시간까지
     * Redisson의 RLock은 expire 메서드가 없으므로, 락을 해제하고 다시 획득하는 방식으로 TTL 갱신
     * 총 실행 시간이 최대 시간을 초과하면 락을 해제하고 Job을 중단하여 무한 락 유지 방지
     */
    private void startHeartbeat(RLock lock, JobExecution jobExecution) {
        heartbeatStartTime = System.currentTimeMillis();
        heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "full-resync-heartbeat");
            t.setDaemon(true);
            return t;
        });

        heartbeatExecutor.scheduleAtFixedRate(() -> {
            try {
                long elapsedSeconds = (System.currentTimeMillis() - heartbeatStartTime) / 1000;
                
                // 총 실행 시간이 최대 시간을 초과하면 락 해제 및 Job 중단
                if (elapsedSeconds >= MAX_JOB_EXECUTION_SECONDS) {
                    log.warn("Full Re-sync Job 최대 실행 시간 초과 ({}초). 락 해제 및 Job 중단", 
                        MAX_JOB_EXECUTION_SECONDS);
                    
                    if (lock.isHeldByCurrentThread()) {
                        lock.unlock();
                        log.info("Full Re-sync 락 해제 완료 (최대 실행 시간 초과)");
                    }
                    
                    // Job을 중단 상태로 변경
                    jobExecution.setStatus(BatchStatus.STOPPED);
                    jobExecution.setExitStatus(ExitStatus.FAILED);
                    
                    heartbeatExecutor.shutdown();
                    return;
                }

                if (lock.isHeldByCurrentThread()) {
                    // Redisson의 RLock은 expire 메서드가 없으므로, 락을 해제하고 다시 획득하여 TTL 갱신
                    // 단, 락을 잃지 않도록 원자적으로 처리
                    lock.unlock();
                    try {
                        boolean renewed = lock.tryLock(0, FULL_RESYNC_LOCK_TTL_SECONDS, TimeUnit.SECONDS);
                        if (renewed) {
                            log.debug("Full Re-sync 락 TTL 갱신: elapsed={}초, TTL={}초", 
                                elapsedSeconds, FULL_RESYNC_LOCK_TTL_SECONDS);
                        } else {
                            log.warn("Full Re-sync 락 TTL 갱신 실패. 다른 인스턴스가 락을 획득했을 수 있습니다.");
                            heartbeatExecutor.shutdown();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        log.error("Heartbeat 중 인터럽트 발생", e);
                        heartbeatExecutor.shutdown();
                    }
                } else {
                    log.warn("현재 스레드가 락을 보유하지 않습니다. Heartbeat 중지");
                    heartbeatExecutor.shutdown();
                }
            } catch (Exception e) {
                log.error("Heartbeat 중 오류 발생", e);
            }
        }, HEARTBEAT_INTERVAL_SECONDS, HEARTBEAT_INTERVAL_SECONDS, TimeUnit.SECONDS);

        log.info("Heartbeat 시작: {}초마다 TTL 갱신, 최대 {}초까지", 
            HEARTBEAT_INTERVAL_SECONDS, MAX_JOB_EXECUTION_SECONDS);
    }

    @Override
    public void afterJob(@NonNull JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();
        log.info("RankingJob 완료 후 락 해제: jobName={}, jobInstanceId={}", 
            jobName, jobExecution.getJobInstance().getInstanceId());

        // Heartbeat 중지
        if (heartbeatExecutor != null && !heartbeatExecutor.isShutdown()) {
            heartbeatExecutor.shutdown();
            try {
                if (!heartbeatExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    heartbeatExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                heartbeatExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        Boolean lockSkipped = (Boolean) jobExecution.getExecutionContext().get("lockSkipped");
        if (Boolean.TRUE.equals(lockSkipped)) {
            log.debug("락이 스킵되었으므로 해제하지 않습니다.");
            return;
        }

        Boolean lockAcquired = (Boolean) jobExecution.getExecutionContext().get("lockAcquired");
        if (!Boolean.TRUE.equals(lockAcquired)) {
            log.debug("락이 획득되지 않았으므로 해제하지 않습니다.");
            return;
        }

        String lockType = (String) jobExecution.getExecutionContext().get("lockType");

        try {
            if (!isRedisAvailable()) {
                log.warn("Redis 연결 실패. 락 해제를 스킵합니다.");
                return;
            }

            if ("fullResync".equals(lockType) && fullResyncLock != null) {
                unlockIfHeld(fullResyncLock, "Full Re-sync");
            } else if ("incremental".equals(lockType) && incrementalLock != null) {
                unlockIfHeld(incrementalLock, "Incremental Update");
            }

        } catch (Exception e) {
            log.error("락 해제 중 오류 발생", e);
            // 예외를 던지지 않음 (Job은 이미 완료됨)
        }
    }

    /**
     * Redis 연결 상태 확인
     */
    private boolean isRedisAvailable() {
        try {
            // 간단한 ping으로 연결 확인
            Boolean result = redissonClient.getBucket("ping").trySet("pong", 1, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            log.debug("Redis 연결 확인 실패", e);
            return false;
        }
    }

    /**
     * Job을 중단 상태로 변경
     */
    private void stopJob(JobExecution jobExecution) {
        jobExecution.setStatus(BatchStatus.STOPPED);
    }

    /**
     * ExecutionContext에 락 정보 설정
     */
    private void setLockContext(JobExecution jobExecution, String lockType) {
        jobExecution.getExecutionContext().put("lockAcquired", true);
        jobExecution.getExecutionContext().put("lockValue", lockValue);
        jobExecution.getExecutionContext().put("lockType", lockType);
    }

    /**
     * 락을 보유하고 있으면 해제
     */
    private void unlockIfHeld(RLock lock, String lockName) {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
            log.info("{} 락 해제 성공: lockValue={}", lockName, lockValue);
        } else {
            log.warn("현재 스레드가 {} 락을 보유하지 않습니다.", lockName);
        }
    }
}

