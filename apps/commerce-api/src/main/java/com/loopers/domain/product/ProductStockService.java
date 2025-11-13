
package com.loopers.domain.product;

import com.loopers.domain.like.LikeRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@RequiredArgsConstructor
@Component
public class ProductStockService {

  public List<Product> deductStock(List<Product> products, Map<Long, Long> quantityMap) {
    products.forEach(i -> i.deductStock(quantityMap.get(i.getId())));
    return products;
  }
}
