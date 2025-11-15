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

  Money totalPrice;

  ZonedDateTime orderAt;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "ref_order_id")
  private List<OrderItem> orderItems = new ArrayList<>();

  public void setOrderItems(List<OrderItem> orderItems) {
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
    this.orderItems = orderItems;
  }

  public static Order create(long refUserId, OrderStatus status, Money totalPrice, List<OrderItem> orderItems) {
    if (status == null) {
      throw new CoreException(ErrorType.BAD_REQUEST, "상태 정보는 비어있을 수 없습니다.");
    }
    return new Order(refUserId, status, totalPrice, orderItems);
  }

  public void cancel() {
    if (status == OrderStatus.CANCELLED) {
      throw new CoreException(ErrorType.BAD_REQUEST, "이미 취소된 주문입니다.");
    }
    this.status = OrderStatus.CANCELLED;
  }

  public void preparing() {
    if (status == OrderStatus.PENDING) {
      throw new CoreException(ErrorType.BAD_REQUEST, "결재대기중 주문입니다.");
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
