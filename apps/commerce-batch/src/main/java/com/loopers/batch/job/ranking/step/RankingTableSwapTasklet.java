package com.loopers.batch.job.ranking.step;

import com.loopers.domain.rank.weekly.WeeklyRankingMV;
import com.loopers.domain.rank.weekly.WeeklyRankingMVRepository;
import com.loopers.domain.rank.weekly.WeeklyRankingWork;
import com.loopers.domain.rank.weekly.WeeklyRankingWorkRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;

@Component
@StepScope
@AllArgsConstructor
public class RankingTableSwapTasklet implements Tasklet {

  private final WeeklyRankingMVRepository mvRepository;
  private final WeeklyRankingWorkRepository workRepository;

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    String baseDate = LocalDate.now().toString();

    mvRepository.deleteAllInBatch();

    List<WeeklyRankingWork> workData = workRepository.findAll();

    List<WeeklyRankingMV> newData = workData.stream()
        .map(work -> WeeklyRankingMV.createFromWork(work, baseDate))
        .toList();

    mvRepository.saveAll(newData);
    return RepeatStatus.FINISHED;
  }
}
