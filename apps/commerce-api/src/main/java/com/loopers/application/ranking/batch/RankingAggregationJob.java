package com.loopers.application.ranking.batch;

import com.loopers.domain.ranking.RankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

// 주간, 월간 랭킹 집계 배치
// read - process - write 패턴을 따릅니다.
// 일간 집계 데이터를 기반으로 주간 및 월간 랭킹을 계산합니다.
// 집계 데이터는 tb_product_metrics 테이블에서 읽어옵니다.
// 주간, 월간 데이터는 tb_mv_product_rank_weekly, tb_mv_product_rank_monthly 뷰에 저장됩니다.
@RequiredArgsConstructor
@Slf4j
@Component
public class RankingAggregationJob {

    private final PlatformTransactionManager transactionManager;
    private final RankingService rankingService;
    private final JobRepository jobRepository;
    /**
     * 주간, 월간 랭킹 집계 실행
     */
    public void execute() {
        log.info("주간 및 월간 랭킹 집계 배치 실행 시작");
        // 1. 일간 집계 데이터 읽기 (tb_product_metrics)
        // 2. 주간 랭킹 계산
        // 3. 월간 랭킹 계산
        // 4. 결과를 tb_mv_product_rank_weekly, tb_mv_product_rank_month

        log.info("주간 및 월간 랭킹 집계 배치 실행 완료");

    }
}
