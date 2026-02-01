package com.loopers.domain.ranking.mv;

import com.loopers.domain.ranking.ProductRankView;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "mv_product_rank_weekly")
@IdClass(WeeklyProductRankId.class)
public class WeeklyProductRank implements ProductRankView {

  @Id
  @Column(name = "ref_product_id", nullable = false)
  private Long refProductId;

  @Id
  @Column(name = "ranking_year_week", nullable = false)
  private String yearWeek;

  @Column(name = "score", nullable = false)
  private Double score;

  @Column(name = "period_start", nullable = false)
  private LocalDate periodStart;

  @Column(name = "period_end", nullable = false)
  private LocalDate periodEnd;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  protected WeeklyProductRank() {}

  public Long getRefProductId() {
    return refProductId;
  }

  public String getYearWeek() {
    return yearWeek;
  }

  public Double getScore() {
    return score;
  }

  public LocalDate getPeriodStart() {
    return periodStart;
  }

  public LocalDate getPeriodEnd() {
    return periodEnd;
  }
}
