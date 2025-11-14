package com.loopers.domain.product;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.money.Money;
import com.loopers.domain.stock.Stock;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Product extends BaseEntity {

  private static final int MAX_NAME_LENGTH = 100;

  @Column(nullable = false, length = MAX_NAME_LENGTH)
  private String name;

  @Embedded
  @AttributeOverride(name = "value", column = @Column(name = "price"))
  private Money price;

  @Column(length = 200)
  private String description;

  @Embedded
  @AttributeOverride(name = "value", column = @Column(name = "stock"))
  private Stock stock;

  @Column(nullable = false)
  private Long likeCount = 0L;

  @Column(name = "ref_brand_id", nullable = false)
  private Long brandId;

  private Product(String name, Money price, String description, Stock stock, Long brandId) {
    validateName(name);
    validatePrice(price);
    validateStock(stock);
    validateBrandId(brandId);
    this.name = name;
    this.price = price;
    this.description = description;
    this.stock = stock;
    this.brandId = brandId;
  }

  public static Product of(String name, Money price, String description, Stock stock, Long brandId) {
    return new Product(name, price, description, stock, brandId);
  }

  private void validateName(String name) {
    if (name == null || name.isBlank()) {
      throw new CoreException(ErrorType.INVALID_PRODUCT_NAME_EMPTY);
    }
    if (name.length() > MAX_NAME_LENGTH) {
      throw new CoreException(ErrorType.INVALID_PRODUCT_NAME_LENGTH);
    }
  }

  private void validatePrice(Money price) {
    if (price == null) {
      throw new CoreException(ErrorType.INVALID_PRODUCT_PRICE_EMPTY);
    }
  }

  private void validateStock(Stock stock) {
    if (stock == null) {
      throw new CoreException(ErrorType.INVALID_PRODUCT_STOCK_EMPTY);
    }
  }

  private void validateBrandId(Long brandId) {
    if (brandId == null) {
      throw new CoreException(ErrorType.INVALID_PRODUCT_BRAND_EMPTY);
    }
  }

  public void decreaseStock(Long amount) {
    this.stock = this.stock.decrease(amount);
  }

  public boolean isAvailable() {
    return stock.isAvailable();
  }

  public boolean isNotAvailable() {
    return !isAvailable();
  }

  public boolean isSameId(Long id) {
    return id != null && id.equals(getId());
  }

  public void validateStockForOrder(Integer requestedQuantity) {
    if (isNotAvailable()) {
      throw new CoreException(ErrorType.INSUFFICIENT_STOCK,
          String.format("상품 [%s]의 재고가 부족합니다.", name));
    }

    if (getStockValue() < requestedQuantity) {
      throw new CoreException(ErrorType.INSUFFICIENT_STOCK,
          String.format("상품 [%s]의 재고가 부족합니다. (요청: %d, 재고: %d)",
              name, requestedQuantity, getStockValue()));
    }
  }

  public void increaseLikeCount() {
    this.likeCount++;
  }

  public void increaseLikeCount(int count) {
    if (count > 0) {
      this.likeCount += count;
    }
  }

  public void decreaseLikeCount() {
    if (this.likeCount > 0) {
      this.likeCount--;
    }
  }

  public Long getPriceValue() {
    return price.getValue();
  }

  public Long getStockValue() {
    return stock.getValue();
  }
}
