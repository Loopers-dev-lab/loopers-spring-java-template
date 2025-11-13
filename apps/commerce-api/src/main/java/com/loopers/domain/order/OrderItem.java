package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

import java.math.BigDecimal;

@Entity
@Table(name = "orderItem")
@Getter
public class OrderItem extends BaseEntity {

  private long refProductId;

  private long quantity;

  private BigDecimal unitPrice;

  private BigDecimal totalPrice;

  protected OrderItem() {
  }

  private OrderItem(long refProductId, long quantity, BigDecimal unitPrice, BigDecimal totalPrice) {
    this.refProductId = refProductId;
    this.quantity = quantity;
    this.unitPrice = unitPrice;
    this.totalPrice = totalPrice;
  }

  public static OrderItem create(long refProductId, long quantity, BigDecimal unitPrice) {
    if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
      throw new CoreException(ErrorType.BAD_REQUEST, "가격은 음수일수 없습니다.");
    }
    BigDecimal totalPrice = unitPrice.multiply(new BigDecimal(quantity));
    return new OrderItem(refProductId, quantity, unitPrice, totalPrice);
  }
}
