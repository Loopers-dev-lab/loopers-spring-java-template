
package com.loopers.domain.order;

import com.loopers.domain.product.Product;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;


public final class OrderPreparer {
  public static List<Product> verifyProductStockDeductable(List<Product> products, Map<Long, Long> quantityMap) {
    products.forEach(i -> i.deductStock(quantityMap.getOrDefault(i.getId(), 0L)));
    return products;
  }

  public static List<Product> verifyProductStockAddable(List<Product> products, Map<Long, Long> quantityMap) {
    products.forEach(i -> i.addStock(quantityMap.get(i.getId())));
    return products;
  }

  public static BigDecimal getTotalAmt(List<Product> products, Map<Long, Long> quantityMap) {
    return products.stream()
        .map(product -> {
          Long productId = product.getId();
          Long quantity = quantityMap.getOrDefault(productId, 0L);
          return product.getPrice().getAmount().multiply(BigDecimal.valueOf(quantity));
        })
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

}
