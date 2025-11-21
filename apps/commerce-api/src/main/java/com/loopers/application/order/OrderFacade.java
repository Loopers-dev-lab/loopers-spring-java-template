package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderCommand.Item;
import com.loopers.domain.order.OrderCommand.PlaceOrder;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
@Transactional
public class OrderFacade {

  private final ProductService productService;
  private final UserService userService;
  private final OrderService orderService;
  private final PointService pointService;

  @Transactional
  public OrderInfo placeOrder(PlaceOrder command) {

    User user = userService.findByIdWithLock(command.userId())
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "유저를 찾을 수 없습니다."));

    List<Long> productIds = command.items().stream()
        .map(Item::productId)
        .toList();

    List<Product> products = productService.getProducts(productIds);

    List<OrderItem> orderItems = buildOrderItems(products, command.items());
    Order order = orderService.createOrder(user.getId(), orderItems);
    long totalAmount = order.getTotalAmount().getValue();

    productService.deductStock(products, orderItems);
    pointService.deductPoint(user, totalAmount);

    return OrderInfo.from(order);
  }

  private List<OrderItem> buildOrderItems(List<Product> products, List<Item> items) {

    Map<Long, Product> productMap = products.stream()
        .collect(Collectors.toMap(Product::getId, p -> p));

    return items.stream()
        .map(i -> new OrderItem(productMap.get(i.productId()), i.quantity()))
        .toList();
  }
}
