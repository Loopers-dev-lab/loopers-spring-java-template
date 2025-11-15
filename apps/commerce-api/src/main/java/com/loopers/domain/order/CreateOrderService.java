
package com.loopers.domain.order;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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

    Money totalPrice = productList.stream().map(product -> {
      long quantity = quantityMap.getOrDefault(product.getId(), 0L);
      Money unitPrice = product.getPrice();
      Money itemTotalPrice = unitPrice.multiply(quantity);

      OrderItem orderItem = OrderItem.create(product.getId(), quantity, unitPrice);
      orderItems.add(orderItem);

      return itemTotalPrice;
    }).reduce(Money.wons(0), Money::add);

    Order order = Order.create(user.getId(), OrderStatus.PAID, totalPrice, orderItems);
    return orderRepository.save(order);
  }

}
