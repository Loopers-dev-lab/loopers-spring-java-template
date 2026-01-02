package com.loopers.batch.job.ranking.step;

import com.loopers.domain.rank.weekly.WeeklyRankingWorkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
@StepScope
@RequiredArgsConstructor
public class RankingPrepareTasklet implements Tasklet {

  private final WeeklyRankingWorkRepository workingRepository;

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    workingRepository.deleteAllInBatch();
    return RepeatStatus.FINISHED;
  }
}
