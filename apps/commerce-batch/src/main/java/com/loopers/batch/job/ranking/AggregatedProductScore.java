package com.loopers.batch.job.ranking;

public record AggregatedProductScore(
    Long refProductId,
    Long totalViewCount,
    Long totalLikeCount,
    Long totalSalesCount
) {

  public double calculateScore(double viewWeight, double likeWeight, double orderWeight) {
    return (totalViewCount * viewWeight) + (totalLikeCount * likeWeight) + (totalSalesCount * orderWeight);
  }
}
