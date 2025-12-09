package com.loopers.application.order;

import com.loopers.application.payment.CreatePaymentCommand;
import com.loopers.application.payment.PaymentFacade;
import com.loopers.domain.coupon.CouponService;
import com.loopers.domain.order.*;
import com.loopers.domain.payment.CardType;
import com.loopers.domain.point.PointRepository;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.stock.StockService;
import com.loopers.domain.user.UserRepository;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@RequiredArgsConstructor
@Component
public class OrderFacade {
  private final UserService userService;
  private final UserRepository userRepository;
  private final ProductService productService;
  private final StockService stockService;
  private final OrderService orderService;
  private final PointService pointService;
  private final CouponService couponService;
  private final PointRepository pointRepository;
  private final PaymentFacade paymentFacade;

  @Transactional(readOnly = true)
  public Page<Order> getOrderList(Long userId,
                                  String sortType,
                                  int page,
                                  int size) {
    return orderService.getOrders(userId, sortType, page, size);
  }

  @Transactional(readOnly = true)
  public OrderInfo getOrderDetail(Long orderId) {
    Order order = orderService.getOrder(orderId);
    return OrderInfo.from(order);
  }

  @Transactional
  public OrderInfo createOrder(CreateOrderCommand command) {
    List<Product> products = productService.getExistingProducts(
        command.orderItemRequests().stream()
            .map(CreateOrderCommand.OrderItemRequest::productId)
            .toList()
    );

    deductStock(command.orderItemRequests());

    Order order = Order.create(command.userId(), createOrderItems(command.orderItemRequests(), products));
    Order savedOrder = orderService.save(order);

    Money finalPrice = applyCouponDiscount(command, order.getTotalPrice());
    //paymentFacade.requestPayment(savedOrder.getId(), finalPrice);
    savedOrder.paid();
    pointService.charge(command.userId(), finalPrice.getAmount().multiply(BigDecimal.valueOf(0.01)));

    return OrderInfo.from(savedOrder);
  }

  private List<OrderItem> createOrderItems(List<CreateOrderCommand.OrderItemRequest> orderItemRequests, List<Product> products) {
    return orderItemRequests.stream()
        .map(item -> {
          Product product = products.stream()
              .filter(p -> p.getId().equals(item.productId()))
              .findFirst()
              .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));
          return OrderItem.create(item.productId(), item.quantity(), product.getPrice());
        })
        .toList();
  }

  private void deductStock(List<CreateOrderCommand.OrderItemRequest> orderItemRequests) {
    orderItemRequests.forEach(item ->
        stockService.deduct(item.productId(), item.quantity()));
  }

  private Money applyCouponDiscount(CreateOrderCommand command, Money originalPrice) {
    if (command.couponId() == null) {
      return originalPrice;
    }

    BigDecimal discountAmount = couponService.useCouponById(
        command.couponId(),
        command.userId(),
        originalPrice
    );

    return originalPrice.subtract(Money.of(discountAmount, originalPrice.getCurrencyCode()));
  }

}
