
package com.loopers.domain.user;

import com.loopers.domain.order.OrderPreparer;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class UserPointService {

  private final PointService pointService;

  @Transactional
  public void use(User user, List<Product> productList, Map<Long, Long> quantityMap) {

    BigDecimal totalAmt = OrderPreparer.getTotalAmt(user.getPoint(), productList, quantityMap);
    pointService.use(user, totalAmt);
  }
}
