package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.stock.StockService;
import com.loopers.application.event.OrderCreatedEvent;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Component
public class OrderFacade {
  private final ProductService productService;
  private final StockService stockService;
  private final OrderService orderService;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional(readOnly = true)
  public Page<Order> getOrderList(Long userId,
                                  String sortType,
                                  int page,
                                  int size) {
    return orderService.getOrders(userId, sortType, page, size);
  }

  @Transactional(readOnly = true)
  public OrderInfo getOrderDetail(String orderId) {
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

    Order order = Order.create(command.userId(), createOrderItems(command.orderItemRequests(), products), command.couponIssueId());
    Order savedOrder = orderService.save(order);

    // 주문 생성 이벤트 발행 (배치 처리용)
    List<OrderCreatedEvent.OrderItemData> orderItemsData = savedOrder.getOrderItems().stream()
        .map(item -> new OrderCreatedEvent.OrderItemData(
            item.getRefProductId(),
            item.getQuantity(),
            item.getUnitPrice().getAmount().longValue()
        ))
        .toList();

    OrderCreatedEvent orderCreatedEvent = new OrderCreatedEvent(
        savedOrder.getOrderId(),
        command.userId(),
        command.couponIssueId(),
        command.cardType(),
        command.cardNo(),
        orderItemsData
    );

    eventPublisher.publishEvent(orderCreatedEvent);

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


}
