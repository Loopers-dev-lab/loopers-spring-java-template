package com.loopers.application.payment;

import com.loopers.domain.order.Money;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentService;
import com.loopers.domain.point.PointService;
import com.loopers.infrastructure.monitoring.PaymentMetricsService;
import com.loopers.interfaces.api.client.PaymentCallbackV1Dto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentFacade {
  private final OrderService orderService;
  private final PointService pointService;
  private final PaymentService paymentService;
  private final PgClient pgClient;
  private final PaymentMetricsService paymentMetricsService;

  public PaymentInfo requestPayment(CreatePaymentCommand command) {
    Payment finalPayment = paymentService.requestPayment(command.orderId(), command.cardType(), command.cardNo(), Money.wons(command.amount()));
    return PaymentInfo.from(finalPayment);
  }

  public PaymentInfo getPayment(String paymentId) {
    return PaymentInfo.from(paymentService.getPayment(paymentId));
  }

  public void handlePaymentCallback(PaymentCallbackV1Dto.CallbackRequest dto) {
    // 결제 상태 업데이트
    paymentService.processPaymentCallback(dto.orderId(), dto.status(), dto.reason());

    // 성공한 경우에만 주문 완료 처리 및 포인트 적립
    if (isPaymentSuccess(dto.status())) {
      // 주문 결제 완료 처리
      orderService.completePayment(dto.orderId());

      // 결제 금액으로 포인트 적립
      Payment payment = paymentService.findPaymentByOrderId(dto.orderId());
      pointService.earnFromPayment(payment.getOrderId(), payment.getAmount());
    }
  }

  private boolean isPaymentSuccess(TransactionStatus status) {
    return status == TransactionStatus.SUCCESS;
  }

  public PgPaymentInfoResponse getPaymentInfoFromPg(String transactionKey) {
    return pgClient.getPaymentInfo(transactionKey);
  }

  public PgPaymentListResponse getPaymentsByOrderFromPg(String orderId) {
    return pgClient.getPaymentsByOrder(orderId);
  }
}

