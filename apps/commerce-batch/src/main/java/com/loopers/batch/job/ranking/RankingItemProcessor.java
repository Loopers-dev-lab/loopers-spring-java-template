package com.loopers.batch.job.ranking;

import com.loopers.batch.config.RankingBatchProperties;
import com.loopers.batch.domain.ranking.MonthlyProductRank;
import com.loopers.batch.domain.ranking.ProductRankEntity;
import com.loopers.batch.domain.ranking.RankingPeriod;
import com.loopers.batch.domain.ranking.WeeklyProductRank;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;

// 점수 계산 + 엔티티 변환
@RequiredArgsConstructor
public class RankingItemProcessor implements ItemProcessor<AggregatedProductScore, ProductRankEntity> {

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

  private final RankingBatchProperties properties;

  private RankingPeriod period;
  private LocalDate baseDate;

  // 주간용 필드
  private String yearWeek;
  private LocalDate weekStart;
  private LocalDate weekEnd;

  // 월간용 필드
  private String yearMonth;
  private LocalDate monthStart;
  private LocalDate monthEnd;

  @BeforeStep
  public void beforeStep(StepExecution stepExecution) {
    String periodParam = stepExecution.getJobParameters().getString("period");
    String baseDateParam = stepExecution.getJobParameters().getString("baseDate");

    this.period = RankingPeriod.fromCode(periodParam);
    this.baseDate = LocalDate.parse(baseDateParam, DATE_FORMATTER);

    if (period == RankingPeriod.WEEKLY) {
      initWeeklyFields();
    } else {
      initMonthlyFields();
    }
  }

  private void initWeeklyFields() {
    WeekFields weekFields = WeekFields.of(Locale.KOREA);
    int weekBasedYear = baseDate.get(weekFields.weekBasedYear());
    this.yearWeek = String.format("%d-W%02d", weekBasedYear, baseDate.get(weekFields.weekOfWeekBasedYear()));
    this.weekStart = baseDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    this.weekEnd = baseDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
  }

  private void initMonthlyFields() {
    this.yearMonth = baseDate.format(DateTimeFormatter.ofPattern("yyyy-MM"));
    this.monthStart = baseDate.withDayOfMonth(1);
    this.monthEnd = baseDate.with(TemporalAdjusters.lastDayOfMonth());
  }

  @Override
  public ProductRankEntity process(AggregatedProductScore item) {
    double score = item.calculateScore(
        properties.getWeight().getView(),
        properties.getWeight().getLike(),
        properties.getWeight().getOrder());

    LocalDateTime now = LocalDateTime.now();

    if (period == RankingPeriod.WEEKLY) {
      return WeeklyProductRank.of(item.refProductId(), yearWeek, score, weekStart, weekEnd, now);
    }
    return MonthlyProductRank.of(item.refProductId(), yearMonth, score, monthStart, monthEnd, now);
  }
}
