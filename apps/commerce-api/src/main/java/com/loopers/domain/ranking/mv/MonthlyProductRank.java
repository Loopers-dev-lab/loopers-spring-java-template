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
@Table(name = "mv_product_rank_monthly")
@IdClass(MonthlyProductRankId.class)
public class MonthlyProductRank implements ProductRankView {

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

  public Long getRefProductId() {
    return refProductId;
  }

  public String getYearMonth() {
    return yearMonth;
  }

  public Double getScore() {
    return score;
  }

}
