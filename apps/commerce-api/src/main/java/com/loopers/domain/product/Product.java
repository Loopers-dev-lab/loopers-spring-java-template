package com.loopers.domain.product;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.brand.Brand;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Getter;

import java.math.BigDecimal;

@Entity
@Table(name = "product")
@Getter
public class Product extends BaseEntity {
  private String name;

  @ManyToOne(fetch = FetchType.LAZY) // 지연 로딩 설정 (성능상 권장)
  @JoinColumn(name = "ref_brand_Id")
  private Brand brand;

  private BigDecimal price;
  private long stock;

  @Transient
  private long likeCount;

  public void setBrand(Brand brand) {
    this.brand = brand;
  }

  public void setLikeCount(long likeCount) {
    this.likeCount = likeCount;
  }

  protected Product() {
    this.stock = 0;
  }

  private Product(Brand brand, String name, BigDecimal price, long stock) {
    this.setBrand(brand);
    this.name = name;
    this.price = price;
    this.stock = stock;
  }

  public static Product create(Brand brand, String name, BigDecimal price, long stock) {
    if (price.compareTo(BigDecimal.ZERO) < 0) {
      throw new CoreException(ErrorType.BAD_REQUEST, "가격은 음수 일수 없습니다.");
    }
    if (stock < 0) {
      throw new CoreException(ErrorType.BAD_REQUEST, "재고는 0보다 커야 합니다.");
    }
    return new Product(brand, name, price, stock);
  }

  public void deductStock(long quantity) {
    if (quantity <= 0) {
      throw new CoreException(ErrorType.BAD_REQUEST, "수량은 0보다 커야 합니다.");
    }
    if (this.stock < quantity) {
      throw new CoreException(ErrorType.INSUFFICIENT_STOCK, "재고가 부족합니다.");
    }
    this.stock -= quantity;
  }

  public void addStock(long quantity) {
    if (quantity <= 0) {
      throw new CoreException(ErrorType.BAD_REQUEST, "수량은 0보다 커야 합니다.");
    }
    this.stock += quantity;
  }
}
