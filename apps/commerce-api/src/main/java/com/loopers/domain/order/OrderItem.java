package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.money.Money;
import com.loopers.domain.product.Product;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_item")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseEntity {

  @Column(name = "ref_product_id", nullable = false)
  private Long productId;


  private Integer quantity;

  @Embedded
  @AttributeOverride(name = "value", column = @Column(name = "price", nullable = false))
  private Money price;

  public OrderItem(Product product, int quantity) {
    if (product.getStock() < quantity) {
      throw new CoreException(ErrorType.NOT_FOUND, "재고가 부족합니다");
    }
    this.productId = product.getId();
    this.quantity = quantity;
    this.price = product.getPrice();
  }

  public long calculateAmount() {
    return price.getValue() * quantity;
  }

  public Long getProductId() {
    return productId;
  }

  public Integer getQuantity() {
    return quantity;
  }

  public Money getPrice() {
    return price;
  }
}
