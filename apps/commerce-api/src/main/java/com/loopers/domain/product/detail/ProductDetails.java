package com.loopers.domain.product.detail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProductDetails {

  private final List<ProductDetail> values;
  private final Map<Long, ProductDetail> detailMap;

  private ProductDetails(List<ProductDetail> values) {
    this.values = values;
    this.detailMap = values.stream()
        .collect(Collectors.toMap(ProductDetail::getProductId, pd -> pd));
  }

  public static ProductDetails from(List<ProductDetail> details) {
    return new ProductDetails(new ArrayList<>(details));
  }

  public ProductDetail get(Long productId) {
    return detailMap.get(productId);
  }

  public List<ProductDetail> toList() {
    return List.copyOf(values);
  }
}
