package com.loopers.batch.job.ranking.step.monthly;

import com.loopers.domain.rank.monthly.MonthlyRankingMV;
import com.loopers.domain.rank.monthly.MonthlyRankingMVRepository;
import com.loopers.domain.rank.monthly.ProductSnapshot;
import com.loopers.domain.rank.weekly.WeeklyRankingWork;
import com.loopers.domain.rank.weekly.WeeklyRankingWorkRepository;
import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MonthlyRankingTableSwapTasklet implements Tasklet {

  private final MonthlyRankingMVRepository monthlyMvRepository;
  private final WeeklyRankingWorkRepository workRepository;
  private final RedisTemplate<String, Object> redisTemplate; // Redis 사용

  @Override
  public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
    monthlyMvRepository.deleteAllInBatch();
    List<WeeklyRankingWork> workData = workRepository.findAll();
    if (workData.isEmpty()) return RepeatStatus.FINISHED;

    List<String> keys = workData.stream()
        .map(work -> "product:snapshot:" + work.getProductId())
        .toList();

    List<Object> snapshots = redisTemplate.opsForValue().multiGet(keys);

    String baseDate = (String) chunkContext.getStepContext().getJobParameters().get("requestDate");
    if (baseDate == null) baseDate = "2026-01";

    String finalBaseDate = baseDate;
    List<MonthlyRankingMV> newData = IntStream.range(0, workData.size())
        .mapToObj(i -> {
          WeeklyRankingWork work = workData.get(i);
          ProductSnapshot snapshot = (ProductSnapshot) snapshots.get(i); // Redis에서 가져온 스냅샷

          if (snapshot == null) {
            return MonthlyRankingMV.createFromWork(work, finalBaseDate, "Unknown", 0L, true);
          }

          return MonthlyRankingMV.createFromWork(
              work,
              "2026-01",
              snapshot.getName(),
              snapshot.getPrice(),
              snapshot.isSoldOut()
          );
        }).toList();

    monthlyMvRepository.saveAll(newData);
    return RepeatStatus.FINISHED;
  }
}
