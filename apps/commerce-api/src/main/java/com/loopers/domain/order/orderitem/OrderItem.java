package com.loopers.domain.order.orderitem;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.order.Order;
import com.loopers.domain.quantity.Quantity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * OrderItem Entity
 * 주문에 포함된 개별 상품
 * 주문 당시의 상품명, 가격, 수량을 스냅샷으로 저장 (Product 변동 영향 없음)
 */
@Entity
@Table(name = "order_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseEntity {

  private static final int MAX_PRODUCT_NAME_LENGTH = 100;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ref_order_id", nullable = false)
  private Order order;

  @Column(name = "ref_product_id", nullable = false)
  private Long productId;

  @Column(name = "product_name", nullable = false, length = MAX_PRODUCT_NAME_LENGTH)
  private String productName;

  @Embedded
  @AttributeOverride(name = "value", column = @Column(name = "quantity", nullable = false))
  private Quantity quantity;

  @Embedded
  @AttributeOverride(name = "value", column = @Column(name = "order_price", nullable = false))
  private OrderPrice orderPrice;

  private OrderItem(Long productId, String productName, Quantity quantity, OrderPrice orderPrice) {
    validateProductId(productId);
    validateProductName(productName);
    validateQuantity(quantity);
    validateOrderPrice(orderPrice);

    this.productId = productId;
    this.productName = productName;
    this.quantity = quantity;
    this.orderPrice = orderPrice;
  }

  /**
   * OrderItem 생성 팩토리 메서드
   * Order는 addItem()을 통해 설정됨
   */
  public static OrderItem of(Long productId, String productName, Quantity quantity, OrderPrice orderPrice) {
    return new OrderItem(productId, productName, quantity, orderPrice);
  }

  /**
   * Order 할당 (양방향 관계 설정용)
   * Order.addItem()에서만 호출되어야 함
   */
  public void assignOrder(Order order) {
    Objects.requireNonNull(order, "Order는 null일 수 없습니다.");
    this.order = order;
  }

  public Long getOrderPriceValue() {
    return orderPrice.getValue();
  }

  public Long getQuantityValue() {
    return quantity.getValue();
  }

  private void validateProductId(Long productId) {
    if (productId == null) {
      throw new CoreException(ErrorType.INVALID_ORDER_ITEM_PRODUCT_EMPTY);
    }
  }

  private void validateProductName(String productName) {
    if (productName == null || productName.isBlank()) {
      throw new CoreException(ErrorType.INVALID_ORDER_ITEM_PRODUCT_NAME_EMPTY);
    }
    if (productName.length() > MAX_PRODUCT_NAME_LENGTH) {
      throw new CoreException(ErrorType.INVALID_ORDER_ITEM_PRODUCT_NAME_LENGTH);
    }
  }

  private void validateQuantity(Quantity quantity) {
    if (quantity == null) {
      throw new CoreException(ErrorType.INVALID_ORDER_ITEM_QUANTITY_EMPTY);
    }
  }

  private void validateOrderPrice(OrderPrice orderPrice) {
    if (orderPrice == null) {
      throw new CoreException(ErrorType.INVALID_ORDER_PRICE_VALUE);
    }
  }
}
