
package com.loopers.domain.product;

import com.loopers.domain.order.OrderPreparer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class ProductStockService {
  private final ProductRepository productRepository;

  public void deduct(List<Product> products, Map<Long, Long> quantityMap) {
    List<Product> deductedProducts = OrderPreparer.verifyProductStockDeductable(products, quantityMap);
    productRepository.save(deductedProducts);
  }

  public void add(List<Product> products, Map<Long, Long> quantityMap) {
    List<Product> addedProducts = OrderPreparer.verifyProductStockAddable(products, quantityMap);
    productRepository.save(addedProducts);
  }

}
