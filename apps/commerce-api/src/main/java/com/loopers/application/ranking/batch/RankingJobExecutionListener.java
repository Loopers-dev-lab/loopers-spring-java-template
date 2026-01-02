package com.loopers.application.ranking.batch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.loopers.domain.ranking.MvProductRankMonthly;
import com.loopers.domain.ranking.MvProductRankMonthlyRepository;
import com.loopers.domain.ranking.MvProductRankWeekly;
import com.loopers.domain.ranking.MvProductRankWeeklyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@JobScope
@RequiredArgsConstructor
public class RankingJobExecutionListener implements JobExecutionListener {

    private final MvProductRankWeeklyRepository weeklyRepository;
    private final MvProductRankMonthlyRepository monthlyRepository;
    private final RankingJsonConverter jsonConverter;

    @Value("#{jobParameters['rankingDate']}")
    private ZonedDateTime rankingDate;

    @Value("#{jobParameters['periodType'] ?: 'weekly'}")
    private String periodType;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        String periodName = "weekly".equals(periodType) ? "주간" : "월간";
        log.info("[RankingJobExecutionListener] beforeJob: {} 랭킹 작업 시작 - rankingDate={}, jobExecutionId={}, thread={}",
                periodName, rankingDate, jobExecution.getId(), Thread.currentThread().getName());
        if (rankingDate == null) {
            log.error("[RankingJobExecutionListener] beforeJob: rankingDate가 NULL입니다! Job parameters: {}",
                    jobExecution.getJobParameters().getParameters());
        }
        if (!"weekly".equals(periodType) && !"monthly".equals(periodType)) {
            log.error("[RankingJobExecutionListener] beforeJob: 잘못된 periodType={}, 'weekly' 또는 'monthly'여야 합니다", periodType);
        }
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        String periodName = "weekly".equals(periodType) ? "주간" : "월간";
        log.info("[RankingJobExecutionListener] afterJob: {} 랭킹 작업 완료. TOP 100 데이터베이스 저장 중", periodName);
        log.info("[RankingJobExecutionListener] afterJob: 작업 실행 상세 - jobExecutionId={}, status={}, exitStatus={}, thread={}",
                jobExecution.getId(), jobExecution.getStatus(), jobExecution.getExitStatus(), Thread.currentThread().getName());

        try {
            ExecutionContext executionContext = jobExecution.getExecutionContext();
            log.info("[RankingJobExecutionListener] afterJob: ExecutionContext 키: {}",
                    executionContext.entrySet().stream().map(Map.Entry::getKey).toList());

            if ("weekly".equals(periodType)) {
                processWeeklyRanking(executionContext, jobExecution);
            } else if ("monthly".equals(periodType)) {
                processMonthlyRanking(executionContext, jobExecution);
            } else {
                log.error("[RankingJobExecutionListener] afterJob: 잘못된 periodType={}, 'weekly' 또는 'monthly'여야 합니다", periodType);
                jobExecution.addFailureException(new IllegalArgumentException("Invalid periodType: " + periodType));
            }

        } catch (Exception e) {
            log.error("[RankingJobExecutionListener] afterJob: {} 랭킹 데이터베이스 저장 실패", periodName, e);
            jobExecution.addFailureException(e);
        }
    }

    private void processWeeklyRanking(ExecutionContext executionContext, JobExecution jobExecution) throws JsonProcessingException {
        Object jsonObj = executionContext.get("weeklyRankingTop100");
        Object countObj = executionContext.get("weeklyRankingCount");

        log.info("[RankingJobExecutionListener] processWeeklyRanking: ExecutionContext에서 조회 - jsonObj={}, countObj={}",
                jsonObj != null ? "not null (type: " + jsonObj.getClass().getName() + ")" : "null",
                countObj != null ? "not null (value: " + countObj + ")" : "null");

        if (jsonObj == null) {
            log.error("[RankingJobExecutionListener] processWeeklyRanking: ExecutionContext에 주간 랭킹 데이터가 없습니다. 스텝이 실행되지 않았을 수 있습니다.");
            log.error("[RankingJobExecutionListener] processWeeklyRanking: ExecutionContext에서 사용 가능한 키: {}",
                    executionContext.entrySet().stream().map(Map.Entry::getKey).toList());
            return;
        }

        String json = jsonObj.toString();
        Integer count = countObj != null ? Integer.parseInt(countObj.toString()) : 0;

        log.info("[RankingJobExecutionListener] processWeeklyRanking: JSON 문자열 길이: {}, 건수: {}", json.length(), count);

        if (json.isEmpty()) {
            log.error("[RankingJobExecutionListener] processWeeklyRanking: ExecutionContext의 주간 랭킹 데이터가 비어있습니다");
            return;
        }

        log.info("[RankingJobExecutionListener] processWeeklyRanking: ExecutionContext에서 {}건 발견", count);
        log.debug("[RankingJobExecutionListener] processWeeklyRanking: JSON 미리보기 (처음 200자): {}",
                json.length() > 200 ? json.substring(0, 200) + "..." : json);

        log.info("[RankingJobExecutionListener] processWeeklyRanking: JSON을 List<MvProductRankWeekly>로 역직렬화 중...");
        List<MvProductRankWeekly> top100 = jsonConverter.fromJsonWeekly(json);
        log.info("[RankingJobExecutionListener] processWeeklyRanking: 역직렬화 완료 - {}건", top100.size());

        if (top100.isEmpty()) {
            log.error("[RankingJobExecutionListener] processWeeklyRanking: 역직렬화된 리스트가 비어있습니다!");
            return;
        }

        for (int i = 0; i < Math.min(3, top100.size()); i++) {
            MvProductRankWeekly item = top100.get(i);
            log.info("[RankingJobExecutionListener] processWeeklyRanking: 아이템[{}] - productId={}, productName={}, brandName={}, score={}, rankingDate={}",
                    i, item.getProductId(), item.getProductName(), item.getBrandName(), item.getScore(), item.getRankingDate());
        }

        log.info("[RankingJobExecutionListener] processWeeklyRanking: {}건에 순위 할당 중...", top100.size());
        for (int i = 0; i < top100.size(); i++) {
            top100.get(i).setRanking(i + 1);
        }
        log.info("[RankingJobExecutionListener] processWeeklyRanking: 순위 할당 완료");

        log.info("[RankingJobExecutionListener] processWeeklyRanking: 기존 주간 랭킹 데이터 삭제 중 - 날짜: {}", rankingDate);
        weeklyRepository.deleteByRankingDate(rankingDate);
        log.info("[RankingJobExecutionListener] processWeeklyRanking: 기존 주간 랭킹 데이터 삭제 완료 - 날짜: {}", rankingDate);

        log.info("[RankingJobExecutionListener] processWeeklyRanking: {}건 데이터베이스 저장 중...", top100.size());
        weeklyRepository.saveAll(top100);
        log.info("[RankingJobExecutionListener] processWeeklyRanking: 주간 랭킹 {}건 데이터베이스 저장 완료", top100.size());

        List<MvProductRankWeekly> saved = weeklyRepository.findTop100ByRankingDateOrderByRankingAsc(rankingDate);
        log.info("[RankingJobExecutionListener] processWeeklyRanking: 검증 - rankingDate={}에 대해 데이터베이스에서 {}건 발견",
                rankingDate, saved.size());
    }

    private void processMonthlyRanking(ExecutionContext executionContext, JobExecution jobExecution) throws JsonProcessingException {
        Object jsonObj = executionContext.get("monthlyRankingTop100");
        Object countObj = executionContext.get("monthlyRankingCount");

        log.info("[RankingJobExecutionListener] processMonthlyRanking: ExecutionContext에서 조회 - jsonObj={}, countObj={}",
                jsonObj != null ? "not null (type: " + jsonObj.getClass().getName() + ")" : "null",
                countObj != null ? "not null (value: " + countObj + ")" : "null");

        if (jsonObj == null) {
            log.error("[RankingJobExecutionListener] processMonthlyRanking: ExecutionContext에 월간 랭킹 데이터가 없습니다. 스텝이 실행되지 않았을 수 있습니다.");
            log.error("[RankingJobExecutionListener] processMonthlyRanking: ExecutionContext에서 사용 가능한 키: {}",
                    executionContext.entrySet().stream().map(Map.Entry::getKey).toList());
            return;
        }

        String json = jsonObj.toString();
        Integer count = countObj != null ? Integer.parseInt(countObj.toString()) : 0;

        log.info("[RankingJobExecutionListener] processMonthlyRanking: JSON 문자열 길이: {}, 건수: {}", json.length(), count);

        if (json.isEmpty()) {
            log.error("[RankingJobExecutionListener] processMonthlyRanking: ExecutionContext의 월간 랭킹 데이터가 비어있습니다");
            return;
        }

        log.info("[RankingJobExecutionListener] processMonthlyRanking: ExecutionContext에서 {}건 발견", count);
        log.debug("[RankingJobExecutionListener] processMonthlyRanking: JSON 미리보기 (처음 200자): {}",
                json.length() > 200 ? json.substring(0, 200) + "..." : json);

        log.info("[RankingJobExecutionListener] processMonthlyRanking: JSON을 List<MvProductRankMonthly>로 역직렬화 중...");
        List<MvProductRankMonthly> top100 = jsonConverter.fromJsonMonthly(json);
        log.info("[RankingJobExecutionListener] processMonthlyRanking: 역직렬화 완료 - {}건", top100.size());

        if (top100.isEmpty()) {
            log.error("[RankingJobExecutionListener] processMonthlyRanking: 역직렬화된 리스트가 비어있습니다!");
            return;
        }

        for (int i = 0; i < Math.min(3, top100.size()); i++) {
            MvProductRankMonthly item = top100.get(i);
            log.info("[RankingJobExecutionListener] processMonthlyRanking: 아이템[{}] - productId={}, productName={}, brandName={}, score={}, rankingDate={}",
                    i, item.getProductId(), item.getProductName(), item.getBrandName(), item.getScore(), item.getRankingDate());
        }

        log.info("[RankingJobExecutionListener] processMonthlyRanking: {}건에 순위 할당 중...", top100.size());
        for (int i = 0; i < top100.size(); i++) {
            top100.get(i).setRanking(i + 1);
        }
        log.info("[RankingJobExecutionListener] processMonthlyRanking: 순위 할당 완료");

        log.info("[RankingJobExecutionListener] processMonthlyRanking: 기존 월간 랭킹 데이터 삭제 중 - 날짜: {}", rankingDate);
        monthlyRepository.deleteByRankingDate(rankingDate);
        log.info("[RankingJobExecutionListener] processMonthlyRanking: 기존 월간 랭킹 데이터 삭제 완료 - 날짜: {}", rankingDate);

        log.info("[RankingJobExecutionListener] processMonthlyRanking: {}건 데이터베이스 저장 중...", top100.size());
        monthlyRepository.saveAll(top100);
        log.info("[RankingJobExecutionListener] processMonthlyRanking: 월간 랭킹 {}건 데이터베이스 저장 완료", top100.size());

        List<MvProductRankMonthly> saved = monthlyRepository.findTop100ByRankingDateOrderByRankingAsc(rankingDate);
        log.info("[RankingJobExecutionListener] processMonthlyRanking: 검증 - rankingDate={}에 대해 데이터베이스에서 {}건 발견",
                rankingDate, saved.size());
    }
}

