package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
public class Order extends BaseEntity {

  private Long refUserId;

  private OrderStatus status;

  private Money totalPrice;

  private ZonedDateTime orderAt;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "ref_order_id")
  private List<OrderItem> orderItems = new ArrayList<>();

  private void setOrderItems(List<OrderItem> orderItems) {
    if (orderItems == null || orderItems.isEmpty()) {
      throw new CoreException(ErrorType.BAD_REQUEST, "주문 상세내역이 없습니다.");
    }
    this.orderItems = orderItems;
  }

  protected Order() {
  }

  private Order(long refUserId, OrderStatus status, Money totalPrice, List<OrderItem> orderItems) {
    this.refUserId = refUserId;
    this.status = status;
    this.totalPrice = totalPrice;
    this.orderAt = ZonedDateTime.now();
    setOrderItems(orderItems);
  }

  public static Order create(long refUserId, List<OrderItem> orderItems) {
    if (orderItems == null || orderItems.isEmpty()) {
      throw new CoreException(ErrorType.BAD_REQUEST, "주문 상세내역이 없습니다.");
    }
    Money totalPrice = orderItems.stream().map(item -> item.getTotalPrice()).reduce(Money.wons(0), Money::add);
    return new Order(refUserId, OrderStatus.PENDING, totalPrice, orderItems);
  }

  public void paid() {
    if (status == OrderStatus.PAID) {
      throw new CoreException(ErrorType.BAD_REQUEST, "이미 결재완료된 주문입니다.");
    }
    this.status = OrderStatus.PAID;
  }

  public void cancel() {
    if (status == OrderStatus.CANCELLED) {
      throw new CoreException(ErrorType.BAD_REQUEST, "이미 취소된 주문입니다.");
    }
    this.status = OrderStatus.CANCELLED;
  }

  public void preparing() {
    if (status == OrderStatus.PENDING) {
      throw new CoreException(ErrorType.BAD_REQUEST, "결제대기중 주문입니다.");
    }
    if (status == OrderStatus.PREPARING) {
      throw new CoreException(ErrorType.BAD_REQUEST, "이미 준비중인 주문입니다.");
    }
    if (status.compare(OrderStatus.PREPARING) > 0) {
      throw new CoreException(ErrorType.BAD_REQUEST, "상품준비 완료된 주문입니다.");
    }
    this.status = OrderStatus.PREPARING;
  }
}
