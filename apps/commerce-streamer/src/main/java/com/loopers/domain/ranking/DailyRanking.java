package com.loopers.domain.ranking;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "daily_ranking", indexes = {
    @Index(name = "idx_daily_ranking_date_rank", columnList = "ranking_date, ranking_position"),
    @Index(name = "idx_daily_ranking_date_product", columnList = "ranking_date, product_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class DailyRanking extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "ranking_date", nullable = false)
  private LocalDate rankingDate;

  @Column(name = "product_id", nullable = false)
  private Long productId;

  @Column(name = "ranking_position", nullable = false)
  private Integer rankingPosition;

  @Column(name = "final_score", nullable = false)
  private Double finalScore;

  public static DailyRanking of(LocalDate rankingDate, Long productId, Integer rankingPosition, Double finalScore) {
    return new DailyRanking(null, rankingDate, productId, rankingPosition, finalScore);
  }
}