
package com.loopers.domain.order;

import com.loopers.domain.point.Point;
import com.loopers.domain.product.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class OrderPreparer {
  public static List<Product> verifyProductStockDeductable(List<Product> products, Map<Long, Long> quantityMap) {
    products.forEach(i -> i.deductStock(quantityMap.get(i.getId())));
    return products;
  }
  public static List<Product> verifyProductStockAddable(List<Product> products, Map<Long, Long> quantityMap) {
    products.forEach(i -> i.addStock(quantityMap.get(i.getId())));
    return products;
  }

  public static BigDecimal getTotalAmt(Point point, List<Product> products, Map<Long, Long> quantityMap) {
    return products.stream()
        .map(product -> {
          Long productId = product.getId();
          Long quantity = quantityMap.getOrDefault(productId, 0L);
          return product.getPrice().multiply(BigDecimal.valueOf(quantity));
        })
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

}
