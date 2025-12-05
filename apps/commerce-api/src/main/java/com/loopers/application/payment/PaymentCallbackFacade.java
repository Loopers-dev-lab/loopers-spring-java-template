package com.loopers.application.payment;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.orderitem.OrderItem;
import com.loopers.domain.order.orderitem.OrderItems;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.point.PointService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.interfaces.api.payment.PaymentCallbackRequest;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCallbackFacade {

  private final PaymentService paymentService;
  private final OrderService orderService;
  private final ProductService productService;
  private final PointService pointService;
  private final Clock clock;

  @Transactional
  public void handleCallback(PaymentCallbackRequest request) {
    Objects.requireNonNull(request, "request는 null일 수 없습니다.");

    if (!request.isSuccess() && !request.isFailed()) {
      log.warn("알 수 없는 결제 상태입니다. transactionKey={}, status={}", request.transactionKey(), request.status());
      return;
    }

    if (request.isSuccess()) {
      handleSuccess(request.transactionKey());
      return;
    }

    handleFailed(request.transactionKey(), request.reason());
  }

  @Transactional
  public void handleSuccess(String transactionKey) {
    Objects.requireNonNull(transactionKey, "transactionKey는 null일 수 없습니다.");
    LocalDateTime completedAt = LocalDateTime.now(clock);

    Payment payment = paymentService.getByTransactionKey(transactionKey);

    if (payment.isCompleted()) {
      log.info("이미 처리된 결제입니다. transactionKey={}", transactionKey);
      return;
    }

    Order order = orderService.getWithItemsById(payment.getOrderId())
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));

    Map<Long, Product> productById = getProductByIdWithLocks(order);
    OrderItems orderItems = new OrderItems(order.getItems());

    if (!orderItems.hasEnoughStock(productById)) {
      handleStockInsufficient(payment, order, completedAt);
      return;
    }

    paymentService.toSuccess(payment, completedAt);
    orderService.completeOrder(order.getId());
    orderItems.decreaseStock(productById);

    log.info("결제 성공 처리 완료. transactionKey={}, orderId={}", transactionKey, order.getId());
  }

  @Transactional
  public void handleFailed(String transactionKey, String reason) {
    Objects.requireNonNull(transactionKey, "transactionKey는 null일 수 없습니다.");
    LocalDateTime completedAt = LocalDateTime.now(clock);

    Payment payment = paymentService.getByTransactionKey(transactionKey);

    if (payment.isCompleted()) {
      log.info("이미 처리된 결제입니다. transactionKey={}", transactionKey);
      return;
    }

    Order order = orderService.getById(payment.getOrderId())
        .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다."));

    paymentService.toFailed(payment, reason, completedAt);
    orderService.failPaymentOrder(order.getId());

    Long pointUsedAmount = order.getPointUsedAmountValue();
    if (pointUsedAmount > 0) {
      pointService.charge(order.getUserId(), pointUsedAmount);
      log.info("포인트 환불 완료. userId={}, amount={}", order.getUserId(), pointUsedAmount);
    }

    log.info("결제 실패 처리 완료. transactionKey={}, orderId={}, reason={}",
        transactionKey, order.getId(), reason);
  }

  private Map<Long, Product> getProductByIdWithLocks(Order order) {
    List<Long> productIds = order.getItems().stream()
        .map(OrderItem::getProductId)
        .distinct()
        .sorted()
        .toList();

    return productService.findByIdsWithLock(productIds).stream()
        .collect(Collectors.toMap(Product::getId, Function.identity()));
  }

  private void handleStockInsufficient(Payment payment, Order order, LocalDateTime completedAt) {
    paymentService.toSuccess(payment, completedAt);
    orderService.failPaymentOrder(order.getId());

    Long pointUsedAmount = order.getPointUsedAmountValue();
    if (pointUsedAmount > 0) {
      pointService.charge(order.getUserId(), pointUsedAmount);
    }

    // TODO: 수동 환불 필요 알림
    log.warn("재고 부족으로 주문 실패 처리. 수동 환불 필요. orderId={}, paymentId={}",
        order.getId(), payment.getId());
  }
}
