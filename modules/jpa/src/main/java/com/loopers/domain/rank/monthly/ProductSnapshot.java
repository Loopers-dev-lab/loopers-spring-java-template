package com.loopers.domain.rank.monthly;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductSnapshot implements Serializable {
  private String name;
  private long price;
  private boolean isSoldOut;

  public static ProductSnapshot of(String name, long price, boolean isSoldOut) {
    return new ProductSnapshot(name, price, isSoldOut);
  }
}
