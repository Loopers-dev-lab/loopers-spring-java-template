package com.loopers.domain.product;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.order.Money;
import com.loopers.domain.stock.Stock;
import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "product")
@Getter
public class Product extends BaseEntity {
  private String name;

  @ManyToOne(fetch = FetchType.LAZY) // 지연 로딩 설정 (성능상 권장)
  @JoinColumn(name = "ref_brand_Id")
  private Brand brand;

  private Money price;

  @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
  @JoinColumn(name = "ref_stock_id", updatable = false)
  private Stock stock;

  public void setBrand(Brand brand) {
    this.brand = brand;
  }

  protected Product() {

  }

  private Product(Brand brand, String name, Money price, Stock stock) {
    this.setBrand(brand);
    this.name = name;
    this.price = price;
    this.stock = stock;
  }

  public static Product create(Brand brand, String name, Money price) {

    return new Product(brand, name, price, Stock.create(null, 0));
  }
}
