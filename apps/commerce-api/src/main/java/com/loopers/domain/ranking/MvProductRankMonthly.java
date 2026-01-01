package com.loopers.domain.ranking;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "mv_product_rank_monthly",
    indexes = {
        @Index(name = "idx_monthly_period_rank", columnList = "period_yyyymm, ranking"),
        @Index(name = "idx_monthly_period", columnList = "period_yyyymm"),
        @Index(name = "idx_monthly_product", columnList = "productId")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_monthly_period_product", columnNames = {"period_yyyymm", "productId"}),
        @UniqueConstraint(name = "uk_monthly_period_rank", columnNames = {"period_yyyymm", "ranking"})
    })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MvProductRankMonthly extends BaseEntity {

  @Column(nullable = false)
  private Long productId;

  @Column(nullable = false, length = 6, name = "period_yyyymm")
  private String yearMonth;

  @Column(nullable = false)
  private Integer ranking;

  @Column(nullable = false)
  private Double score;

  @Column(nullable = false)
  private Integer likeCount = 0;

  @Column(nullable = false)
  private Integer orderCount = 0;

  @Column(nullable = false)
  private Integer viewCount = 0;

  public MvProductRankMonthly(Long productId, String yearMonth, Integer ranking, Double score,
                              Integer likeCount, Integer orderCount, Integer viewCount) {
    this.productId = productId;
    this.yearMonth = yearMonth;
    this.ranking = ranking;
    this.score = score;
    this.likeCount = likeCount;
    this.orderCount = orderCount;
    this.viewCount = viewCount;
  }

  public static MvProductRankMonthly create(Long productId, String yearMonth, Integer ranking,
                                            Integer likeCount, Integer orderCount, Integer viewCount) {
    final double VIEW_WEIGHT = 0.1;
    final double LIKE_WEIGHT = 0.2;
    final double ORDER_WEIGHT = 0.6;

    double score = (VIEW_WEIGHT * viewCount) + (LIKE_WEIGHT * likeCount) + (ORDER_WEIGHT * orderCount);

    return new MvProductRankMonthly(productId, yearMonth, ranking, score, likeCount, orderCount, viewCount);
  }
}
