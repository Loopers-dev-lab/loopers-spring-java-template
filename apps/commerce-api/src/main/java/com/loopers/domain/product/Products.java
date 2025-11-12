package com.loopers.domain.product;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Products {

  private final List<Product> values;

  private Products(List<Product> values) {
    this.values = values;
  }

  public static Products from(List<Product> products) {
    return new Products(new ArrayList<>(products));
  }

  public List<Long> getBrandIds() {
    return values.stream()
        .map(Product::getBrandId)
        .distinct()
        .toList();
  }

  public List<Long> getProductIds() {
    return values.stream()
        .map(Product::getId)
        .toList();
  }

  public Optional<Product> findById(Long productId) {
    return values.stream()
        .filter(p -> p.isSameId(productId))
        .findFirst();
  }

  public List<Product> toList() {
    return List.copyOf(values);
  }
}
