package com.loopers.domain.rank.monthly;

import com.loopers.domain.rank.weekly.WeeklyRankingWork;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "mv_product_rank_monthly")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MonthlyRankingMV {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private String baseDate;
  private Long productId;
  private Double totalScore;
  private Integer currentRank;

  private String productName;
  private Long price;
  private boolean isSoldOut;

  public static MonthlyRankingMV createFromWork(WeeklyRankingWork work, String baseDate, String productName, Long price, boolean isSoldOut) {
    MonthlyRankingMV mv = new MonthlyRankingMV();
    mv.baseDate = baseDate;
    mv.productId = work.getProductId();
    mv.totalScore = work.getScore();
    mv.currentRank = work.getRanking();

    mv.productName = productName;
    mv.price = price;
    mv.isSoldOut = isSoldOut;

    return mv;
  }
}
