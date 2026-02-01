package com.loopers.batch.domain.ranking;

import java.util.Arrays;

public enum RankingPeriod {
  WEEKLY("weekly", "주간 랭킹"),
  MONTHLY("monthly", "월간 랭킹");

  private final String code;
  private final String description;

  RankingPeriod(String code, String description) {
    this.code = code;
    this.description = description;
  }

  public String getCode() {
    return code;
  }

  public String getDescription() {
    return description;
  }

  public static RankingPeriod fromCode(String code) {
    return Arrays.stream(values())
        .filter(period -> period.code.equalsIgnoreCase(code))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(
            "유효하지 않은 period: " + code + ". WEEKLY 또는 MONTHLY만 허용됩니다."));
  }
}
