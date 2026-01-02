package com.loopers.interfaces.api.rank;

import com.loopers.application.rank.RankingInfo;

public record RankingV1Dto() {

  public record RankingResponse(
      Long productId,
      String productName,
      Long price,
      boolean isSoldOut,
      int currentRank
  ) {

    public static RankingResponse from(RankingInfo info) {
      return new RankingResponse(
          info.productId(),
          info.productName(),
          info.price(),
          info.isSoldOut(),
          info.currentRank()
          );
    }
  }
}
