package com.loopers.domain.ranking.mv;

import java.io.Serializable;
import java.util.Objects;

public class WeeklyProductRankId implements Serializable {

  private Long refProductId;
  private String yearWeek;

  public WeeklyProductRankId() {}

  public WeeklyProductRankId(Long refProductId, String yearWeek) {
    this.refProductId = refProductId;
    this.yearWeek = yearWeek;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    WeeklyProductRankId that = (WeeklyProductRankId) o;
    return Objects.equals(refProductId, that.refProductId) && Objects.equals(yearWeek, that.yearWeek);
  }

  @Override
  public int hashCode() {
    return Objects.hash(refProductId, yearWeek);
  }
}
