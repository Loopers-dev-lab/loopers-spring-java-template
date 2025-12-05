package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.money.Money;
import com.loopers.domain.order.orderitem.OrderItem;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Order Entity (Aggregate Root)
 * 사용자의 주문 정보
 * OrderItem의 생명주기를 관리
 */
@Entity
@Table(name = "orders",
    indexes = {
        @Index(name = "idx_order_user_id", columnList = "ref_user_id"),
        @Index(name = "idx_order_user_ordered_at", columnList = "ref_user_id, ordered_at DESC")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

  @Column(name = "ref_user_id", nullable = false)
  private Long userId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OrderStatus status;

  @Embedded
  @AttributeOverride(name = "value", column = @Column(name = "total_amount"))
  private Money totalAmount;

  @Embedded
  @AttributeOverride(name = "value", column = @Column(name = "point_used_amount"))
  private Money pointUsedAmount;

  @Embedded
  @AttributeOverride(name = "value", column = @Column(name = "pg_amount"))
  private Money pgAmount;

  @Column(name = "ordered_at", nullable = false)
  private LocalDateTime orderedAt;

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<OrderItem> items = new ArrayList<>();

  private Order(Long userId, OrderStatus status, Money totalAmount, Money pointUsedAmount, Money pgAmount, LocalDateTime orderedAt) {
    validateUserId(userId);
    validateStatus(status);
    validateTotalAmount(totalAmount);
    validateOrderedAt(orderedAt);
    validateTotalAmount(totalAmount, pointUsedAmount, pgAmount);

    this.userId = userId;
    this.status = status;
    this.totalAmount = totalAmount;
    this.pointUsedAmount = pointUsedAmount;
    this.pgAmount = pgAmount;
    this.orderedAt = orderedAt;
  }

  public static Order of(Long userId, OrderStatus status, Long totalAmount, Long pointUsedAmount, Long pgAmount, LocalDateTime orderedAt) {
    return new Order(userId, status, Money.of(totalAmount), Money.of(pointUsedAmount), Money.of(pgAmount), orderedAt);
  }

  public Long getTotalAmountValue() {
    return totalAmount.getValue();
  }

  public Long getPointUsedAmountValue() {
    return pointUsedAmount != null ? pointUsedAmount.getValue() : 0L;
  }

  public Long getPgAmountValue() {
    return pgAmount != null ? pgAmount.getValue() : 0L;
  }

  public boolean hasPgAmount() {
    return getPgAmountValue() > 0;
  }

  public void addItem(OrderItem item) {
    Objects.requireNonNull(item, "OrderItem은 null일 수 없습니다.");
    this.items.add(item);
    item.assignOrder(this);
  }


  public void complete() {
    if (this.status != OrderStatus.PENDING) {
      throw new CoreException(ErrorType.ORDER_CANNOT_COMPLETE);
    }
    this.status = OrderStatus.COMPLETED;
  }

  public void failPayment() {
    if (this.status != OrderStatus.PENDING) {
      throw new CoreException(ErrorType.ORDER_CANNOT_FAIL_PAYMENT);
    }
    this.status = OrderStatus.PAYMENT_FAILED;
  }

  public void retryComplete() {
    if (this.status != OrderStatus.PAYMENT_FAILED) {
      throw new CoreException(ErrorType.ORDER_CANNOT_RETRY_COMPLETE);
    }
    this.status = OrderStatus.COMPLETED;
  }

  private void validateUserId(Long userId) {
    if (userId == null) {
      throw new CoreException(ErrorType.INVALID_ORDER_USER_EMPTY);
    }
  }

  private void validateStatus(OrderStatus status) {
    if (status == null) {
      throw new CoreException(ErrorType.INVALID_ORDER_STATUS_EMPTY);
    }
  }

  private void validateTotalAmount(Money totalAmount) {
    if (totalAmount == null) {
      throw new CoreException(ErrorType.INVALID_ORDER_TOTAL_AMOUNT_EMPTY);
    }
  }

  private void validateOrderedAt(LocalDateTime orderedAt) {
    if (orderedAt == null) {
      throw new CoreException(ErrorType.INVALID_ORDER_ORDERED_AT_EMPTY);
    }
  }

  private void validateTotalAmount(Money totalAmount, Money pointUsedAmount, Money pgAmount) {
    if (!totalAmount.isSameValue(pointUsedAmount.add(pgAmount))) {
      throw new CoreException(ErrorType.INVALID_ORDER_AMOUNT_MISMATCH);
    }
  }
}
