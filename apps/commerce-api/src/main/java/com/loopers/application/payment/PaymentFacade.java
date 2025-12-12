package com.loopers.application.payment;

import com.loopers.domain.order.Money;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentService;
import com.loopers.application.event.PaymentCallbackEvent;
import com.loopers.infrastructure.monitoring.PaymentMetricsService;
import com.loopers.interfaces.api.client.PaymentCallbackV1Dto;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentFacade {
  private final PaymentService paymentService;
  private final PgClient pgClient;
  private final PaymentMetricsService paymentMetricsService;
  private final ApplicationEventPublisher eventPublisher;

  public PaymentInfo requestPayment(CreatePaymentCommand command) {
    Payment finalPayment = paymentService.requestPayment(command.orderId(), command.cardType(), command.cardNo(), Money.wons(command.amount()));
    return PaymentInfo.from(finalPayment);
  }

  public PaymentInfo getPayment(Long paymentId) {
    return PaymentInfo.from(paymentService.getPayment(paymentId));
  }

  public void handlePaymentCallback(PaymentCallbackV1Dto.CallbackRequest dto) {
    // 결제 상태 업데이트
    paymentService.processPaymentCallback(dto.orderId(), dto.status(), dto.reason());

    // 이벤트 발행 (후속 처리는 EventHandler에서)
    eventPublisher.publishEvent(new PaymentCallbackEvent(dto.orderId(), dto.amount(), dto.status(), dto.reason()));
  }


  public PgPaymentInfoResponse getPaymentInfoFromPg(String transactionKey) {
    return pgClient.getPaymentInfo(transactionKey);
  }

  public PgPaymentListResponse getPaymentsByOrderFromPg(String orderId) {
    return pgClient.getPaymentsByOrder(orderId);
  }
}

