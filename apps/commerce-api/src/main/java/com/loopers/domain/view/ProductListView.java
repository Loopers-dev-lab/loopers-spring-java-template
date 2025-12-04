package com.loopers.domain.view;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.product.Product;
import com.loopers.domain.stock.Stock;
import com.querydsl.core.annotations.Immutable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;

import java.math.BigDecimal;

@Entity
@Immutable
@Table(name = "product_list_view",
    indexes = {
        @Index(name = "idx_brand_like", columnList = "brand_id, like_count DESC")
    })
@Getter
public class ProductListView extends BaseEntity {

  @Column(name = "product_id", nullable = false, unique = true)
  private Long productId;
  private Long brandId;
  private String name;
  private BigDecimal price;
  private Long stockQuantity;
  private Long likeCount;


  protected ProductListView() {
  }

  private ProductListView(Long productId, Long brandId, String name, BigDecimal price,
                          Long stockQuantity, Long likeCount) {
    this.productId = productId;
    this.brandId = brandId;
    this.name = name;
    this.price = price;
    this.stockQuantity = stockQuantity;
    this.likeCount = likeCount;
  }

  public static ProductListView create(Product product, Stock stock) {
    return new ProductListView(product.getId(), product.getBrand().getId(), product.getName(), product.getPrice().getAmount(), stock.getAvailable(), 0L);
  }
}
