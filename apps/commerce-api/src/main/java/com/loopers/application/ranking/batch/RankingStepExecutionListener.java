package com.loopers.application.ranking.batch;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.metrics.product.ProductMetricsDailyAggregated;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.ranking.MvProductRankMonthly;
import com.loopers.domain.ranking.MvProductRankWeekly;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.*;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class RankingStepExecutionListener implements StepExecutionListener {

    private final ProductService productService;
    private final BrandService brandService;
    private final RankingJsonConverter jsonConverter;

    @Value("#{jobParameters['rankingDate']}")
    private ZonedDateTime rankingDate;

    @Value("#{jobParameters['periodType'] ?: 'weekly'}")
    private String periodType; // "weekly" or "monthly"

    @Override
    public void beforeStep(StepExecution stepExecution) {
        String periodName = "weekly".equals(periodType) ? "주간" : "월간";
        log.info("[RankingStepExecutionListener] beforeStep: {} 랭킹 스텝 시작 - rankingDate={}, stepName={}, thread={}",
                periodName, rankingDate, stepExecution.getStepName(), Thread.currentThread().getName());
        if (rankingDate == null) {
            log.error("[RankingStepExecutionListener] beforeStep: rankingDate가 NULL입니다! Job parameters: {}", 
                    stepExecution.getJobExecution().getJobParameters().getParameters());
        }
        if (!"weekly".equals(periodType) && !"monthly".equals(periodType)) {
            log.error("[RankingStepExecutionListener] beforeStep: 잘못된 periodType={}, 'weekly' 또는 'monthly'여야 합니다", periodType);
        }
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        String periodName = "weekly".equals(periodType) ? "주간" : "월간";
        log.info("[RankingStepExecutionListener] afterStep: {} 랭킹 스텝 완료. PriorityQueue에서 TOP 100 추출 중", periodName);
        log.info("[RankingStepExecutionListener] afterStep: 스텝 실행 상세 - stepName={}, readCount={}, writeCount={}, commitCount={}, thread={}",
                stepExecution.getStepName(), stepExecution.getReadCount(), stepExecution.getWriteCount(),
                stepExecution.getCommitCount(), Thread.currentThread().getName());

        try {
            log.info("[RankingStepExecutionListener] afterStep: RankingProcessor에서 PriorityQueue 가져오는 중...");
            PriorityQueue<ProductMetricsDailyAggregated> queue = RankingProcessor.getTop100Queue();
            
            if (queue == null) {
                log.error("[RankingStepExecutionListener] afterStep: PriorityQueue가 null입니다!");
                return stepExecution.getExitStatus();
            }
            
            if (queue.isEmpty()) {
                log.warn("[RankingStepExecutionListener] afterStep: PriorityQueue가 비어있습니다!");
                return stepExecution.getExitStatus();
            }

            log.info("[RankingStepExecutionListener] afterStep: PriorityQueue 가져오기 성공 - size={}", queue.size());
            List<ProductMetricsDailyAggregated> allItems = new ArrayList<>(queue);
            log.info("[RankingStepExecutionListener] afterStep: PriorityQueue에서 총 {}건 조회", allItems.size());
            
            for (int i = 0; i < Math.min(5, allItems.size()); i++) {
                ProductMetricsDailyAggregated item = allItems.get(i);
                log.info("[RankingStepExecutionListener] afterStep: 아이템[{}] - productId={}, score={}, likeCount={}, viewCount={}, soldCount={}",
                        i, item.getProductId(), item.calculateScore(), item.getTotalLikeCount(),
                        item.getTotalViewCount(), item.getTotalSoldCount());
            }

            log.info("[RankingStepExecutionListener] afterStep: 점수 기준 내림차순 정렬 중...");
            allItems.sort(Comparator.comparingDouble(ProductMetricsDailyAggregated::calculateScore).reversed());
            log.info("[RankingStepExecutionListener] afterStep: 정렬 완료. 상위 3개 점수: {}",
                    allItems.stream().limit(3).map(item -> item.calculateScore()).toList());
            List<ProductMetricsDailyAggregated> top100 = allItems.stream()
                    .limit(100)
                    .toList();

            log.info("[RankingStepExecutionListener] afterStep: TOP 100 선택 완료 (실제 건수: {})", top100.size());
            if ("weekly".equals(periodType)) {
                log.info("[RankingStepExecutionListener] afterStep: {}건에 대해 MvProductRankWeekly 객체 생성 중...", top100.size());
                List<MvProductRankWeekly> weeklyRankings = createWeeklyRankings(top100);
                String json = jsonConverter.toJson(weeklyRankings);
                log.info("[RankingStepExecutionListener] afterStep: JSON 변환 완료 - 길이={} 문자", json.length());
                
                ExecutionContext jobExecutionContext = stepExecution.getJobExecution().getExecutionContext();
                jobExecutionContext.put("weeklyRankingTop100", json);
                jobExecutionContext.put("weeklyRankingCount", weeklyRankings.size());
                
                log.info("[RankingStepExecutionListener] afterStep: 주간 집계 {}건 저장됨 to JobExecution ExecutionContext", weeklyRankings.size());
                
            } else if ("monthly".equals(periodType)) {
                log.info("[RankingStepExecutionListener] afterStep: {}건에 대해 MvProductRankMonthly 객체 생성 중...", top100.size());
                List<MvProductRankMonthly> monthlyRankings = createMonthlyRankings(top100);
                String json = jsonConverter.toJson(monthlyRankings);
                log.info("[RankingStepExecutionListener] afterStep: JSON 변환 완료 - 길이={} 문자", json.length());
                
                ExecutionContext jobExecutionContext = stepExecution.getJobExecution().getExecutionContext();
                jobExecutionContext.put("monthlyRankingTop100", json);
                jobExecutionContext.put("monthlyRankingCount", monthlyRankings.size());
                
                log.info("[RankingStepExecutionListener] afterStep: 월간 집계 {}건 저장됨 to JobExecution ExecutionContext", monthlyRankings.size());
                
            } else {
                log.error("[RankingStepExecutionListener] afterStep: 잘못된 periodType={}, 'weekly' 또는 'monthly'여야 합니다", periodType);
                stepExecution.addFailureException(new IllegalArgumentException("Invalid periodType: " + periodType));
                return ExitStatus.FAILED;
            }

        } catch (Exception e) {
            log.error("[RankingStepExecutionListener] afterStep: {} 랭킹 처리 실패", periodName, e);
            stepExecution.addFailureException(e);
            return ExitStatus.FAILED;
        } finally {
            RankingProcessor.clearTop100Queue();
            log.info("[RankingStepExecutionListener] afterStep: RankingProcessor의 PriorityQueue 초기화 완료");
        }

        return stepExecution.getExitStatus();
    }

    private List<MvProductRankWeekly> createWeeklyRankings(List<ProductMetricsDailyAggregated> top100) {
        int successCount = 0;
        int failureCount = 0;
            List<MvProductRankWeekly> weeklyRankings = new ArrayList<>();
        
        for (int i = 0; i < top100.size(); i++) {
            ProductMetricsDailyAggregated aggregated = top100.get(i);
                try {
                log.debug("[RankingStepExecutionListener] afterStep: 아이템[{}] 처리 중 - productId={}", i, aggregated.getProductId());
                    Product product = productService.getProductById(aggregated.getProductId())
                            .orElseThrow(() -> new RuntimeException("Product not found: " + aggregated.getProductId()));
                    Brand brand = brandService.getBrandById(product.getBrandId());

                    MvProductRankWeekly weekly = new MvProductRankWeekly();
                    weekly.setProductId(aggregated.getProductId());
                    weekly.setProductName(product.getName());
                    weekly.setBrandId(product.getBrandId());
                    weekly.setBrandName(brand.getName());
                    weekly.setScore(aggregated.calculateScore());
                    weekly.setLikeCount(aggregated.getTotalLikeCount().intValue());
                    weekly.setViewCount(aggregated.getTotalViewCount().intValue());
                    weekly.setOrderCount(aggregated.getTotalSoldCount().intValue());
                    weekly.setRankingDate(rankingDate);

                if (weekly.getRankingDate() == null) {
                    log.error("[RankingStepExecutionListener] afterStep: 아이템[{}]의 rankingDate가 NULL입니다 - productId={}", 
                            i, weekly.getProductId());
                }

                    weeklyRankings.add(weekly);
                successCount++;
                
                if (i < 3) {
                    log.info("[RankingStepExecutionListener] afterStep: MvProductRankWeekly[{}] 생성 완료 - productId={}, productName={}, brandName={}, score={}, rankingDate={}",
                            i, weekly.getProductId(), weekly.getProductName(), weekly.getBrandName(), weekly.getScore(), weekly.getRankingDate());
                }

            } catch (Exception e) {
                failureCount++;
                log.error("[RankingStepExecutionListener] afterStep: productId={}에 대한 MvProductRankWeekly 생성 실패",
                        aggregated.getProductId(), e);
            }
        }
        
        log.info("[RankingStepExecutionListener] afterStep: MvProductRankWeekly 생성 완료 - 성공: {}, 실패: {}, 총: {}",
                successCount, failureCount, weeklyRankings.size());
        
        return weeklyRankings;
    }

    private List<MvProductRankMonthly> createMonthlyRankings(List<ProductMetricsDailyAggregated> top100) {
        int successCount = 0;
        int failureCount = 0;
        List<MvProductRankMonthly> monthlyRankings = new ArrayList<>();
        
        for (int i = 0; i < top100.size(); i++) {
            ProductMetricsDailyAggregated aggregated = top100.get(i);
            try {
                log.debug("[RankingStepExecutionListener] afterStep: 아이템[{}] 처리 중 - productId={}", i, aggregated.getProductId());
                Product product = productService.getProductById(aggregated.getProductId())
                        .orElseThrow(() -> new RuntimeException("Product not found: " + aggregated.getProductId()));
                Brand brand = brandService.getBrandById(product.getBrandId());

                MvProductRankMonthly monthly = new MvProductRankMonthly();
                monthly.setProductId(aggregated.getProductId());
                monthly.setProductName(product.getName());
                monthly.setBrandId(product.getBrandId());
                monthly.setBrandName(brand.getName());
                monthly.setScore(aggregated.calculateScore());
                monthly.setLikeCount(aggregated.getTotalLikeCount().intValue());
                monthly.setViewCount(aggregated.getTotalViewCount().intValue());
                monthly.setOrderCount(aggregated.getTotalSoldCount().intValue());
                monthly.setRankingDate(rankingDate);

                if (monthly.getRankingDate() == null) {
                    log.error("[RankingStepExecutionListener] afterStep: 아이템[{}]의 rankingDate가 NULL입니다 - productId={}", 
                            i, monthly.getProductId());
                }

                monthlyRankings.add(monthly);
                successCount++;
                
                if (i < 3) {
                    log.info("[RankingStepExecutionListener] afterStep: MvProductRankMonthly[{}] 생성 완료 - productId={}, productName={}, brandName={}, score={}, rankingDate={}",
                            i, monthly.getProductId(), monthly.getProductName(), monthly.getBrandName(), monthly.getScore(), monthly.getRankingDate());
                }

            } catch (Exception e) {
                failureCount++;
                log.error("[RankingStepExecutionListener] afterStep: productId={}에 대한 MvProductRankMonthly 생성 실패",
                        aggregated.getProductId(), e);
            }
        }
        
        log.info("[RankingStepExecutionListener] afterStep: MvProductRankMonthly 생성 완료 - 성공: {}, 실패: {}, 총: {}",
                successCount, failureCount, monthlyRankings.size());
        
        return monthlyRankings;
    }
}
