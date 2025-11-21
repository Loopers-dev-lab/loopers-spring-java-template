package com.loopers.domain.product;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.money.Money;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

  @Column(nullable = false)
  private Long brandId;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String description;

  @Column(nullable = false)
  @Embedded
  private Money price;

  private int stock;

  private int likeCount;

  @Version
  private Long version;


  public Product(Long brandId, String name, String description, Money price, int stock) {
    if (brandId == null) {
      throw new CoreException(ErrorType.BAD_REQUEST, "상품의 브랜드를 등록해야 합니다.");
    }

    if (name == null || name.isBlank()) {
      throw new CoreException(ErrorType.BAD_REQUEST, "상품의 이름을 등록해야 합니다.");
    }

    if (description == null || description.isBlank()) {
      throw new CoreException(ErrorType.BAD_REQUEST, "상품의 설명을 등록해야 합니다.");
    }

    if (price == null) {
      throw new CoreException(ErrorType.BAD_REQUEST, "상품의 가격을 등록해야 합니다.");
    }
    if (stock < 0) {
      throw new CoreException(ErrorType.BAD_REQUEST, "상품의 재고는 음수가 될 수 없습니다.");
    }

    this.brandId = brandId;
    this.name = name;
    this.description = description;
    this.price = price;
    this.stock = stock;
  }

  public Long getBrandId() {
    return brandId;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public Money getPrice() {
    return price;
  }

  public int getStock() {
    return stock;
  }

  public int getLikeCount() {
    return likeCount;
  }

  public void deductStock(int quantity) {
    this.stock -= quantity;
  }

  public int increaseLikeCount() {
    this.likeCount++;
    return this.likeCount;
  }

  public int decreaseLikeCount() {
    if (this.likeCount < 0) {
      throw new CoreException(ErrorType.BAD_REQUEST, "좋아요수는 0보다 작을 수 없습니다.");
    }
    this.likeCount--;
    return this.likeCount;

  }

}
