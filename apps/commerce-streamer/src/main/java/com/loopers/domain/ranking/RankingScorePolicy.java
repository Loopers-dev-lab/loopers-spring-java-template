package com.loopers.domain.ranking;

import com.loopers.domain.event.EventType;

public class RankingScorePolicy {
    public static final int ZERO_SCORE = 0;
    private final double view;
  private final double like;
  private final double order;

  public RankingScorePolicy(double view, double like, double order) {
    this.view = view;
    this.like = like;
    this.order = order;
  }

  public double calculateScore(String eventCode, int quantity) {
    if (EventType.PRODUCT_VIEWED.matches(eventCode)) return view;
    if (EventType.PRODUCT_LIKED.matches(eventCode)) return like;
    if (EventType.PRODUCT_UNLIKED.matches(eventCode)) return -like;
    if (EventType.PRODUCT_SOLD.matches(eventCode)) return order * quantity;
    return ZERO_SCORE;
  }
}
