package com.loopers.batch.domain.ranking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "mv_product_rank_monthly")
@IdClass(MonthlyProductRankId.class)
public class MonthlyProductRank implements ProductRankEntity {

  @Id
  @Column(name = "ref_product_id", nullable = false)
  private Long refProductId;

  @Id
  @Column(name = "ranking_year_month", nullable = false)
  private String yearMonth;

  @Column(name = "score", nullable = false)
  private Double score;

  @Column(name = "period_start", nullable = false)
  private LocalDate periodStart;

  @Column(name = "period_end", nullable = false)
  private LocalDate periodEnd;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  protected MonthlyProductRank() {}

  private MonthlyProductRank(
      Long refProductId,
      String yearMonth,
      Double score,
      LocalDate periodStart,
      LocalDate periodEnd,
      LocalDateTime updatedAt) {
    this.refProductId = refProductId;
    this.yearMonth = yearMonth;
    this.score = score;
    this.periodStart = periodStart;
    this.periodEnd = periodEnd;
    this.updatedAt = updatedAt;
  }

  public static MonthlyProductRank of(
      Long refProductId,
      String yearMonth,
      Double score,
      LocalDate periodStart,
      LocalDate periodEnd,
      LocalDateTime updatedAt) {
    return new MonthlyProductRank(refProductId, yearMonth, score, periodStart, periodEnd, updatedAt);
  }

  public String getYearMonth() {
    return yearMonth;
  }

  public Double getScore() {
    return score;
  }
}
