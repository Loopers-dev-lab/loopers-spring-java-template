package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "order_item")
@Getter
public class OrderItem extends BaseEntity {

  private long refProductId;

  private long quantity;
  @Embedded
  @AttributeOverride(
      name = "amount",
      column = @Column(name = "unit_price_amount")
  )
  @AttributeOverride(
      name = "currency",
      column = @Column(name = "unit_price_currency")
  )
  private Money unitPrice;

  @Embedded
  @AttributeOverride(
      name = "amount",
      column = @Column(name = "total_price_amount")
  )
  @AttributeOverride(
      name = "currency",
      column = @Column(name = "total_price_currency")
  )
  private Money totalPrice;

  protected OrderItem() {
  }

  private OrderItem(long refProductId, long quantity, Money unitPrice) {
    this.refProductId = refProductId;
    this.quantity = quantity;
    this.unitPrice = unitPrice;
    this.totalPrice = unitPrice.multiply(quantity);
  }

  public static OrderItem create(long refProductId, long quantity, Money unitPrice) {
    if (quantity <= 0) {
      throw new CoreException(ErrorType.BAD_REQUEST, "수량은 0보다 커야 합니다.");
    }
    return new OrderItem(refProductId, quantity, unitPrice);
  }

  public Money getTotalPrice() {
    return this.totalPrice;
  }
}
