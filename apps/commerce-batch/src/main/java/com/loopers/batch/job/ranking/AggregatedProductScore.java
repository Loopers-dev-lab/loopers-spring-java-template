package com.loopers.batch.job.ranking;

public record AggregatedProductScore(
    Long refProductId,
    Long totalViewCount,
    Long totalLikeCount,
    Long totalSalesCount
) {

  public AggregatedProductScore {
    totalViewCount = totalViewCount != null ? totalViewCount : 0L;
    totalLikeCount = totalLikeCount != null ? totalLikeCount : 0L;
    totalSalesCount = totalSalesCount != null ? totalSalesCount : 0L;
  }

  public double calculateScore(double viewWeight, double likeWeight, double orderWeight) {
    return (totalViewCount * viewWeight) + (totalLikeCount * likeWeight) + (totalSalesCount * orderWeight);
  }
}
