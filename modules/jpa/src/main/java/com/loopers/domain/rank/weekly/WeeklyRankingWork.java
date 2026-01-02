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
@Table(name = "weekly_ranking_work")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WeeklyRankingWork {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;
  private Long productId;
  private Double score;
  private Integer ranking;

  public WeeklyRankingWork(Long productId, Double score, Integer ranking) {
    this.productId = productId;
    this.score = score;
    this.ranking = ranking;
  }
}
