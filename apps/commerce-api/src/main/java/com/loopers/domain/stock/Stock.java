package com.loopers.domain.stock;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.product.Product;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "stock")
@Getter
public class Stock extends BaseEntity {
  private Long refProductId;
  private long available = 0;

  protected Stock() {

  }

  private Stock(Long refProductId, long available) {
    this.refProductId = refProductId;
    this.available = available;
  }

  public static Stock create(Long refProductId, long available) {
    if (available < 0) {
      throw new CoreException(ErrorType.BAD_REQUEST, "재고는 0보다 커야 합니다.");
    }
    return new Stock(refProductId, available);
  }

  public void deduct(long quantity) {
    if (quantity <= 0) {
      throw new CoreException(ErrorType.BAD_REQUEST, "수량은 0보다 커야 합니다.");
    }
    if (this.available < quantity) {
      throw new CoreException(ErrorType.INSUFFICIENT_STOCK, "재고가 부족합니다.");
    }
    this.available -= quantity;
  }
}
