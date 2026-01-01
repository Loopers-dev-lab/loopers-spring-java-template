package com.loopers.application.ranking;

public record RankInfo(Integer rank, Double score) {
  
  public static RankInfo of(Integer rank, Double score) {
    return new RankInfo(rank, score);
  }
  
  public static RankInfo empty() {
    return new RankInfo(null, null);
  }
}