package com.loopers.application.rank;

import com.loopers.domain.product.Product;
import com.loopers.domain.rank.monthly.MonthlyRankingMV;
import com.loopers.domain.rank.weekly.WeeklyRankingMV;

public record RankingInfo(
    Long productId,
    String productName,
    Long price,
    boolean isSoldOut,
    int currentRank
) {

  public static RankingInfo of(Product product, int currentRank) {
    return new RankingInfo(
        product.getId(),
        product.getName(),
        product.getPrice().getValue(),
        product.getStock() <= 0,
        currentRank
    );
  }

  public static RankingInfo from(WeeklyRankingMV mv) {
    return new RankingInfo(
        mv.getProductId(),
        mv.getProductName(),
        mv.getPrice(),
        mv.isSoldOut(),
        mv.getCurrentRank()
    );
  }

  public static RankingInfo from(MonthlyRankingMV mv) {
    return new RankingInfo(
        mv.getProductId(),
        mv.getProductName(),
        mv.getPrice(),
        mv.isSoldOut(),
        mv.getCurrentRank()
    );
  }
}
