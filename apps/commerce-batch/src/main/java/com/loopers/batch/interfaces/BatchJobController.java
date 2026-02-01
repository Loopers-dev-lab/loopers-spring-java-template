package com.loopers.batch.interfaces;

import com.loopers.batch.application.BatchJobFacade;
import com.loopers.batch.application.BatchJobFacade.JobExecutionResult;
import com.loopers.batch.domain.ranking.RankingPeriod;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

//  인증/권한 체크 필요
@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class BatchJobController {

  private final BatchJobFacade batchJobFacade;

  @PostMapping("/ranking")
  public ResponseEntity<JobExecutionResult> runRankingAggregation(
      @RequestParam RankingPeriod period,
      @RequestParam @DateTimeFormat(pattern = "yyyyMMdd") LocalDate date
  ) {
    return ResponseEntity.ok(batchJobFacade.runRankingAggregation(period, date));
  }
}
