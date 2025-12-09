package com.loopers.application.payment;

import com.loopers.domain.order.Money;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentService;
import com.loopers.infrastructure.monitoring.PaymentMetricsService;
import com.loopers.interfaces.api.client.PaymentCallbackV1Dto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentFacade {

  private final PaymentService paymentService;
  private final PgClient pgClient;
  private final PaymentMetricsService paymentMetricsService;

  public PaymentInfo requestPayment(CreatePaymentCommand command) {

    // 1) 도메인 초기 상태 저장
    Payment payment = Payment.create(command.orderId()
        , command.cardType(), command.cardNo()
        , Money.wons(command.amount()));
    Payment saved = paymentService.createPendingPayment(payment);

    // 2) PG 요청 (외부 통신)
    PgPayRequest pgRequest = new PgPayRequest(
        command.orderId().toString(),
        command.cardType().toString(),
        command.cardNo(),
        command.amount(),
        command.callbackUrl()
    );

    PgPayResponse pgResponse = pgClient.requestPayment(pgRequest);

    // 3) PG 결과 반영하여 최종 상태 저장
    Payment finalPayment = paymentService.updateWithPgResult(saved, pgResponse);

    return PaymentInfo.from(finalPayment);
  }

  public PaymentInfo getPayment(String paymentId) {
    return PaymentInfo.from(paymentService.getPayment(paymentId));
  }

  public void handlePaymentCallback(PaymentCallbackV1Dto.CallbackRequest dto) {
    try {
      // 콜백 로그 저장
      Payment payment = paymentService.getPaymentByOrderId(dto.orderId());
      boolean success = dto.reason() == null || !dto.reason().toLowerCase().contains("error");
      paymentMetricsService.recordPaymentCallback(success, payment.getCardType().name());

      // 결제 상태
      payment.approved();
      paymentService.updatePaymentFromCallback(payment);
    } catch (Exception e) {
      // 콜백 처리 실패 시 에러 로그
      try {
        Payment payment = paymentService.getPaymentByOrderId(dto.orderId());
        paymentMetricsService.recordPaymentError("/api/v1/payments/callback", "general", payment.getCardType().name());
      } catch (Exception ignored) {
        // 로그 저장 실패는 무시
      }
      throw e;
    }
  }

  public PgPaymentInfoResponse getPaymentInfoFromPg(String transactionKey) {
    return pgClient.getPaymentInfo(transactionKey);
  }

  public PgPaymentListResponse getPaymentsByOrderFromPg(String orderId) {
    return pgClient.getPaymentsByOrder(orderId);
  }
}

