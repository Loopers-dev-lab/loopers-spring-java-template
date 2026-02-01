package com.loopers.domain.ranking.mv;

import java.io.Serializable;
import java.util.Objects;

public class MonthlyProductRankId implements Serializable {

  private Long refProductId;
  private String yearMonth;

  public MonthlyProductRankId() {}

  public MonthlyProductRankId(Long refProductId, String yearMonth) {
    this.refProductId = refProductId;
    this.yearMonth = yearMonth;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MonthlyProductRankId that = (MonthlyProductRankId) o;
    return Objects.equals(refProductId, that.refProductId) && Objects.equals(yearMonth, that.yearMonth);
  }

  @Override
  public int hashCode() {
    return Objects.hash(refProductId, yearMonth);
  }
}
