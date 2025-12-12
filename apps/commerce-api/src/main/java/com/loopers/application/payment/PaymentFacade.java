package com.loopers.application.payment;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.orderitem.OrderItem;
import com.loopers.domain.order.orderitem.OrderItems;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.infrastructure.pg.PgClient;
import com.loopers.infrastructure.pg.PgPaymentRequest;
import com.loopers.infrastructure.pg.PgPaymentResponse;
import com.loopers.infrastructure.pg.PgRequestFailedException;
import com.loopers.interfaces.api.order.OrderDto.PaymentInfo;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentFacade {

  private final PaymentService paymentService;
  private final OrderService orderService;
  private final ProductService productService;
  private final PgClient pgClient;
  private final Clock clock;

  @Value("${pg-client.callback-url}")
  private String callbackUrl;

  @Transactional
  public void processPayment(Order order, PaymentInfo paymentInfo) {
    Objects.requireNonNull(order, "order는 null일 수 없습니다.");

    if (!order.hasPgAmount()) {
      completeOrder(order);
      return;
    }

    requestPgPayment(order, paymentInfo);
  }

  private void completeOrder(Order order) {
    Map<Long, Product> productById = getProductByIdWithLocks(order);
    OrderItems orderItems = new OrderItems(order.getItems());

    orderService.completeOrder(order.getId(), LocalDateTime.now(clock));
    orderItems.decreaseStock(productById);

    log.info("포인트 전액 결제로 즉시 완료. orderId={}", order.getId());
  }

  private void requestPgPayment(Order order, PaymentInfo paymentInfo) {
    if (paymentInfo == null) {
      log.warn("PG 결제가 필요하지만 결제 정보가 없습니다. orderId={}", order.getId());
      return;
    }

    Long pgAmount = order.getPgAmountValue();
    LocalDateTime requestedAt = LocalDateTime.now(clock);
    Payment payment = paymentService.create(
        order.getId(), order.getUserId(), paymentInfo.cardType(), paymentInfo.cardNo(), pgAmount, requestedAt
    );

    try {
      PgPaymentResponse pgResponse = pgClient.requestPayment(
          String.valueOf(order.getUserId()),
          PgPaymentRequest.of(order.getId(), paymentInfo.cardType(), paymentInfo.cardNo(), pgAmount, callbackUrl)
      );

      if (pgResponse.isPending()) {
        paymentService.toPending(payment.getId(), pgResponse.transactionKey());
        log.info("PG 결제 요청 완료. orderId={}, transactionKey={}", order.getId(), pgResponse.transactionKey());
      }
    } catch (PgRequestFailedException e) {
      paymentService.toRequestFailed(payment.getId());
      log.error("PG 결제 요청 실패. orderId={}, paymentId={}", order.getId(), payment.getId(), e);
      throw e;
    }
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
}
