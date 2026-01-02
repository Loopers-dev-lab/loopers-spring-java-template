package com.loopers.config.batch;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.batch.ranking.RankingType;
import com.loopers.batch.ranking.dto.ProductScore5MinDto;
import com.loopers.batch.ranking.step1.Aggregate5MinProcessor;
import com.loopers.batch.ranking.step1.Aggregate5MinReader;
import com.loopers.batch.ranking.step1.Aggregate5MinWriter;
import com.loopers.batch.ranking.step2.RankingUpdateTasklet;
import com.loopers.batch.ranking.step2.RankingUpdateService;
import com.loopers.batch.ranking.step3.Tier1SyncTasklet;
import com.loopers.batch.ranking.step3.Tier2SyncTasklet;
import com.loopers.domain.ranking.RankingEventLog;
import com.loopers.infrastructure.ranking.ProductScore5MinJpaRepository;
import com.loopers.infrastructure.ranking.RankingSnapshotDailyJpaRepository;
import com.loopers.infrastructure.ranking.RankingSnapshotHourlyJpaRepository;
import com.loopers.infrastructure.ranking.RankingSnapshotMonthlyJpaRepository;
import com.loopers.infrastructure.ranking.RankingSnapshotWeeklyJpaRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.Flow;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Arrays;
import java.util.function.Function;

/**
 * 상품 랭킹 업데이트 Job Configuration
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ProductRankingUpdateJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    
    // Step 1 Components
    private final Aggregate5MinReader aggregate5MinReader;
    private final Aggregate5MinProcessor aggregate5MinProcessor;
    private final Aggregate5MinWriter aggregate5MinWriter;
    
    // Step 2 Components
    private final ProductScore5MinJpaRepository productScore5MinJpaRepository;
    private final RankingUpdateService rankingUpdateService;
    
    // Step 3 Components
    private final StringRedisTemplate redisTemplate;
    private final RankingSnapshotHourlyJpaRepository hourlySnapshotRepository;
    private final RankingSnapshotDailyJpaRepository dailySnapshotRepository;
    private final RankingSnapshotWeeklyJpaRepository weeklySnapshotRepository;
    private final RankingSnapshotMonthlyJpaRepository monthlySnapshotRepository;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;
    
    // Listener
    private final RankingJobExecutionListener rankingJobExecutionListener;

    private static final int CHUNK_SIZE = 500;

    /**
     * ProductRankingUpdateJob 정의
     */
    @Bean
    public Job productRankingUpdateJob() {
        return new JobBuilder("productRankingUpdateJob", jobRepository)
            .start(aggregate5MinStep())
            .next(parallelRankingUpdateStep())
            .next(parallelTier1SyncStep())
            .next(unifiedTier2SyncStep())
            .listener(rankingJobExecutionListener)
            .build();
    }

    /**
     * Step 1: 5분 단위 Raw Metrics 집계
     */
    @Bean
    public Step aggregate5MinStep() {
        return new StepBuilder("aggregate5MinStep", jobRepository)
            .<RankingEventLog, ProductScore5MinDto>chunk(CHUNK_SIZE, transactionManager)
            .reader(aggregate5MinReader)
            .processor(aggregate5MinProcessor)
            .writer(aggregate5MinWriter)
            .build();
    }

    /**
     * Step 2: 병렬 랭킹 업데이트 Flow (4개 타입 병렬 처리)
     * 
     * Spring Batch 5.x 공식 문서 확인 결과:
     * - StepBuilder.flow() 메서드는 실제로 존재하며, FlowStepBuilder를 반환합니다.
     * - 이는 StepBuilder의 메서드로, Flow를 Step으로 감싸는 기능을 제공합니다.
     * - 공식 문서에서는 FlowStepBuilder를 직접 사용하는 것을 권장하지만,
     *   StepBuilder.flow()를 통한 접근도 유효한 방법입니다.
     */
    @Bean
    public Step parallelRankingUpdateStep() {
        return createParallelStep(
            "parallelRankingUpdateStep",
            "parallelRankingUpdateFlow",
            "RankingUpdateStep",
            this::createRankingUpdateTasklet
        );
    }

    /**
     * 랭킹 타입별 업데이트 Tasklet 생성
     */
    private Tasklet createRankingUpdateTasklet(RankingType rankingType) {
        return new RankingUpdateTasklet(
            rankingType,
            productScore5MinJpaRepository,
            rankingUpdateService
        );
    }
    
    /**
     * Step들을 사용하여 병렬 Flow를 생성하는 헬퍼 메서드
     * Spring Batch 5.x 공식 문서에 따르면 FlowBuilder는 org.springframework.batch.core.job.builder.FlowBuilder에 위치
     * Step을 Flow로 변환한 후 병렬 실행
     */
    private Flow createParallelFlow(String flowName, Step... steps) {
        Flow[] flows = Arrays.stream(steps)
            .map(step -> new FlowBuilder<Flow>(step.getName() + "Flow").start(step).build())
            .toArray(Flow[]::new);
        
        return new FlowBuilder<Flow>(flowName)
            .split(taskExecutor())
            .add(flows)
            .build();
    }

    /**
     * Step 3-1: 병렬 Tier 1 동기화 (4개 타입 병렬 처리)
     * 
     * StepBuilder.flow()를 사용하여 Flow를 Step으로 감싸는 방식 사용
     */
    @Bean
    public Step parallelTier1SyncStep() {
        return createParallelStep(
            "parallelTier1SyncStep",
            "parallelTier1Flow",
            "Tier1SyncStep",
            this::createTier1SyncTasklet
        );
    }

    /**
     * 랭킹 타입별 Tier 1 동기화 Tasklet 생성
     */
    private Tasklet createTier1SyncTasklet(RankingType rankingType) {
        return new Tier1SyncTasklet(
            rankingType,
            redisTemplate,
            hourlySnapshotRepository,
            dailySnapshotRepository,
            weeklySnapshotRepository,
            monthlySnapshotRepository
        );
    }

    /**
     * 병렬 Step 생성 헬퍼 메서드
     * 랭킹 타입별로 Step을 생성하고 병렬 Flow로 묶어서 반환
     */
    private Step createParallelStep(String stepName, String flowName, String stepNameSuffix, 
                                    Function<RankingType, Tasklet> taskletFactory) {
        Step[] steps = Arrays.stream(RankingType.values())
            .map(rankingType -> {
                String individualStepName = rankingType.name().toLowerCase() + stepNameSuffix;
                return new StepBuilder(individualStepName, jobRepository)
                    .tasklet(taskletFactory.apply(rankingType), transactionManager)
                    .build();
            })
            .toArray(Step[]::new);
        
        Flow parallelFlow = createParallelFlow(flowName, steps);
        
        return new StepBuilder(stepName, jobRepository)
            .flow(parallelFlow)
            .build();
    }

    /**
     * Step 3-2: 통합 Tier 2 동기화
     */
    @Bean
    public Step unifiedTier2SyncStep() {
        return new StepBuilder("unifiedTier2SyncStep", jobRepository)
            .tasklet(unifiedTier2SyncTasklet(), transactionManager)
            .build();
    }

    @Bean
    public Tasklet unifiedTier2SyncTasklet() {
        return new Tier2SyncTasklet(
            redisTemplate,
            entityManager,
            objectMapper
        );
    }

    /**
     * 병렬 처리용 TaskExecutor
     */
    @Bean
    public TaskExecutor taskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
        executor.setConcurrencyLimit(4); // 4개 타입 병렬 처리
        executor.setThreadNamePrefix("ranking-update-");
        return executor;
    }
}

