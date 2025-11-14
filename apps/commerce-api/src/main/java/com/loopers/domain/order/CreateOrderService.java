
package com.loopers.domain.order;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class CreateOrderService {
  private final ProductRepository productRepository;
  private final OrderRepository orderRepository;

  public Order save(User user, List<Product> productList, Map<Long, Long> quantityMap) {

    List<OrderItem> orderItems = new ArrayList<>();

    BigDecimal totalPrice = productList.stream().map(product -> {
      long quantity = quantityMap.get(product.getId());
      BigDecimal unitPrice = product.getPrice();
      BigDecimal itemTotalPrice = unitPrice.multiply(new BigDecimal(quantity));

      OrderItem orderItem = OrderItem.create(product.getId(), quantity, unitPrice);
      orderItems.add(orderItem);

      return itemTotalPrice;
    }).reduce(BigDecimal.ZERO, BigDecimal::add);

    Order order = Order.create(user.getId(), OrderStatus.PAID, totalPrice, orderItems);
    return orderRepository.save(order);
  }

}
