package com.loopers.batch.job.productRankingJob;

import java.time.LocalDate;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductRankingJobScheduler {

	private final JobLauncher jobLauncher;
	private final Job weeklyRankingJob;
	private final Job monthlyRankingJob;

	// 매일 01:10에 전일(anchorDate=어제) 기준으로 주/월 MV를 갱신
	@Scheduled(cron = "0 10 1 * * *")
	public void runWeeklyRanking() {
		runWithAnchor(weeklyRankingJob, LocalDate.now().minusDays(1));
	}

	@Scheduled(cron = "0 20 1 * * *")
	public void runMonthlyRanking() {
		runWithAnchor(monthlyRankingJob, LocalDate.now().minusDays(1));
	}

	private void runWithAnchor(Job job, LocalDate anchor) {
		try {
			JobParameters params = new JobParametersBuilder()
					.addString("anchorDate", anchor.toString()) // yyyy-MM-dd
					.addLong("ts", System.currentTimeMillis())   // 재실행 구분자
					.toJobParameters();
			log.info("Launching job={} anchorDate={}", job.getName(), anchor);
			jobLauncher.run(job, params);
		} catch (Exception e) {
			log.error("Failed to launch job={}", job.getName(), e);
		}
	}
}


