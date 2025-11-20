package com.loopers.application.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 좋아요 수 동기화 스케줄러.
 * <p>
 * 주기적으로 Spring Batch Job을 실행하여 Like 테이블의 COUNT(*) 결과를 Product.likeCount 필드에 동기화합니다.
 * </p>
 * <p>
 * <b>동작 원리:</b>
 * <ol>
 *   <li>주기적으로 실행 (기본: 5초마다)</li>
 *   <li>Spring Batch Job 실행</li>
 *   <li>Reader: 모든 상품 ID 조회</li>
 *   <li>Processor: 각 상품의 좋아요 수 집계 (Like 테이블 COUNT(*))</li>
 *   <li>Writer: Product 테이블의 likeCount 필드 업데이트</li>
 * </ol>
 * </p>
 * <p>
 * <b>설계 근거:</b>
 * <ul>
 *   <li><b>Spring Batch 사용:</b> 대량 처리, 청크 단위 처리, 재시작 가능</li>
 *   <li><b>Eventually Consistent:</b> 좋아요 수는 약간의 지연 허용 가능</li>
 *   <li><b>성능 최적화:</b> 조회 시 COUNT(*) 대신 컬럼만 읽으면 됨</li>
 *   <li><b>쓰기 경합 최소화:</b> Like 테이블은 Insert-only로 쓰기 경합 없음</li>
 *   <li><b>확장성:</b> Redis 없이도 대규모 트래픽 처리 가능</li>
 * </ul>
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class LikeCountSyncScheduler {

    private final JobLauncher jobLauncher;
    private final Job likeCountSyncJob;

    /**
     * 좋아요 수를 동기화합니다.
     * <p>
     * 5초마다 실행되어 Spring Batch Job을 통해 Like 테이블의 집계 결과를 Product.likeCount에 반영합니다.
     * </p>
     * <p>
     * <b>Spring Batch 장점:</b>
     * <ul>
     *   <li><b>청크 단위 처리:</b> 100개씩 묶어서 처리하여 성능 최적화</li>
     *   <li><b>트랜잭션 관리:</b> 청크 단위로 커밋하여 안정성 보장</li>
     *   <li><b>재시작 가능:</b> Job 실패 시 재시작 가능</li>
     *   <li><b>모니터링:</b> Spring Batch 메타데이터로 실행 이력 추적</li>
     * </ul>
     * </p>
     * <p>
     * <b>주기적 실행 전략:</b>
     * <ul>
     *   <li><b>타임스탬프 기반 JobParameters:</b> 매 실행마다 타임스탬프를 추가하여 새로운 JobInstance 생성</li>
     *   <li><b>5초마다 실행:</b> 스케줄러가 5초마다 Job을 실행하여 좋아요 수를 최신화</li>
     * </ul>
     * </p>
     */
    @Scheduled(fixedDelay = 5000) // 5초마다 실행
    public void syncLikeCounts() {
        try {
            log.debug("좋아요 수 동기화 배치 Job 시작");

            // 타임스탬프를 JobParameters에 추가하여 매번 새로운 JobInstance 생성
            // Spring Batch는 동일한 JobParameters를 가진 JobInstance를 재실행하지 않으므로,
            // 타임스탬프를 추가하여 매 실행마다 새로운 JobInstance를 생성합니다.
            JobParameters jobParameters = new JobParametersBuilder()
                .addString("jobName", "likeCountSync")
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

            // Spring Batch Job 실행
            JobExecution jobExecution = jobLauncher.run(likeCountSyncJob, jobParameters);

            log.debug("좋아요 수 동기화 배치 Job 완료: status={}", jobExecution.getStatus());

        } catch (JobRestartException e) {
            log.error("좋아요 수 동기화 배치 Job 재시작 실패", e);
        } catch (Exception e) {
            log.error("좋아요 수 동기화 배치 Job 실행 중 오류 발생", e);
        }
    }
}

