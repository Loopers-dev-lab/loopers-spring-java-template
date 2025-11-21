package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.money.Money;
import com.loopers.domain.product.Product;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

  @Column(name = "ref_user_id", nullable = false)
  private Long userId;

  @Embedded
  @AttributeOverride(name = "value", column = @Column(name = "total_amount"))
  private Money totalAmount;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "order_id", nullable = false)
  private List<OrderItem> orderItems = new java.util.ArrayList<>();

  public Order(Long userId, List<OrderItem> orderItems) {
    if (userId == null) {
      throw new CoreException(ErrorType.BAD_REQUEST, "사용자 ID는 필수입니다.");
    }

    if (orderItems == null || orderItems.isEmpty()) {
      throw new CoreException(ErrorType.BAD_REQUEST, "주문 아이템은 필수입니다.");
    }
    this.userId = userId;
    this.orderItems = orderItems;
    this.totalAmount = new Money(calculateTotalAmount());
  }

  public Long getUserId() {
    return userId;
  }

  public void addOrderItem(Product product, int quantity) {

    if (product == null) {
      throw new IllegalArgumentException("상품 정보가 필요합니다");
    }
    if (quantity <= 0) {
      throw new IllegalArgumentException("주문 수량은 0보다 커야 합니다");
    }

    orderItems.add(new OrderItem(product, quantity));

  }

  public List<OrderItem> getOrderItems() {
    return orderItems;
  }

  public long calculateTotalAmount() {
    return orderItems.stream()
        .mapToLong(OrderItem::calculateAmount)
        .sum();
  }

  public Money getTotalAmount() {
    return totalAmount;
  }
}
