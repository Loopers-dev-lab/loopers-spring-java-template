package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
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

  private OrderItem(long refProductId, long quantity, Money unitPrice, Money totalPrice) {
    this.refProductId = refProductId;
    this.quantity = quantity;
    this.unitPrice = unitPrice;
    this.totalPrice = totalPrice;
  }

  public static OrderItem create(long refProductId, long quantity, Money unitPrice) {
    Money totalPrice = unitPrice.multiply(quantity);
    return new OrderItem(refProductId, quantity, unitPrice, totalPrice);
  }
}
