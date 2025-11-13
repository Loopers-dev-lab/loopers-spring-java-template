package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.ProductStockService;
import com.loopers.domain.user.UserModel;
import com.loopers.domain.user.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Component
public class OrderFacade {
  private final UserService userService;
  private final PointService pointService;
  private final ProductService productService;
  private final OrderService orderService;

  private final ProductStockService productStockService;

  public Page<Order> getOrderList(Long userId,
                                  String sortType,
                                  int page,
                                  int size) {
    return orderService.getOrders(userId, sortType, page, size);
  }

  public OrderInfo getOrderDetail(Long orderId) {
    Order order = orderService.getOrder(orderId);
    return OrderInfo.from(order);
  }

  @Transactional
  public OrderInfo createOrder(Long userId, Map<Long, Long> productQuantityMap) {
    //사용자 존재 확인
    UserModel user = userService.getUser(userId);

    //상품 존재 확인
    List<Product> productList = productService.getExistingProducts(productQuantityMap.keySet());

    //재고확인및 차감
    List<Product> deductedProducts = productStockService.deductStock(productList, productQuantityMap);
    productService.save(deductedProducts);

    //포인트 확인 및 차감
    BigDecimal totalPrice = deductedProducts.stream()
        .map(Product::getPrice)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
    pointService.use(user, totalPrice);

    //주문 생성
    Order order = Order.create(userId, OrderStatus.PAID, totalPrice, totalPrice, ZonedDateTime.now());
    order.setOrderItems(deductedProducts.stream().map(i -> OrderItem.create(i.getId(), productQuantityMap.get(i.getId()), i.getPrice())).toList());
    Order savedOrder = orderService.save(order);

    return OrderInfo.from(savedOrder);
  }
}
