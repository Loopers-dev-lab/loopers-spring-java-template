package com.loopers.domain.rank.weekly;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "mv_product_rank_weekly")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class WeeklyRankingMV {
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

  private WeeklyRankingMV(String baseDate, Long productId, Double totalScore, Integer currentRank,
      String productName, Long price, boolean isSoldOut) {
    this.baseDate = baseDate;
    this.productId = productId;
    this.totalScore = totalScore;
    this.currentRank = currentRank;
    this.productName = productName;
    this.price = price;
    this.isSoldOut = isSoldOut;
  }

  // 정적 팩토리 메서드 (의미 있는 생성 방식 제공)
  public static WeeklyRankingMV createFromWork(WeeklyRankingWork work, String baseDate) {
    return new WeeklyRankingMV(
        baseDate,
        work.getProductId(),
        work.getScore(),
        work.getRanking(),
        "상품명 임시", // 실제 구현 시 Product 정보 결합 필요
        0L,          // 실제 구현 시 Product 정보 결합 필요
        false        // 실제 구현 시 Product 정보 결합 필요
    );
  }
}
