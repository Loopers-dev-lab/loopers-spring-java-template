package com.loopers.infrastructure.feign;

import com.loopers.application.payment.*;
import com.loopers.domain.order.Money;
import com.loopers.domain.payment.CardType;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentService;
import com.loopers.infrastructure.monitoring.PaymentMetricsService;
import com.loopers.interfaces.api.ApiResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
@RequiredArgsConstructor
public class PgClientImpl implements PgClient {

  private static final String CUSTOMER_USER_ID = "135135";

  private final FeignPgClient feignPgClient;
  private final PaymentMetricsService paymentMetricsService;
  private final PaymentService paymentService;

  @Override
  @CircuitBreaker(name = "pgCircuit", fallbackMethod = "fallbackRequest")
  public PgPayResponse requestPayment(String orderId, CardType cardType, String cardNo, Money price) {
    paymentMetricsService.recordPaymentRequest("/api/v1/payments", cardType.name());
    Payment payment = paymentService.requestPayment(orderId, cardType, cardNo, price);

    try {
      PgPayRequest request = new PgPayRequest(
          orderId,
          cardType.name(),
          cardNo,
          price.getAmount()
      );
      var apiResponse = feignPgClient.requestPayment(CUSTOMER_USER_ID, request);

      if (apiResponse.meta().result() != ApiResponse.Metadata.Result.SUCCESS) {
        String errorMessage = "PG 요청 실패: " + apiResponse.meta().message();
        paymentMetricsService.recordPaymentError("/api/v1/payments", "pg_error", cardType);
        throw new PgApiException(errorMessage);
      }

      PgPayResponse response = apiResponse.data();
      paymentService.processPaymentRequest(payment, response.isSuccess(), response.transactionKey());
      paymentMetricsService.recordPaymentResponse("/api/v1/payments", response.status(), cardType);
      return response;
    } catch (PgApiException e) {
      throw e;
    } catch (Exception e) {
      paymentMetricsService.recordPaymentException("/api/v1/payments", e, cardType);
      throw e;
    }
  }

  @Override
  @CircuitBreaker(name = "pgCircuit", fallbackMethod = "fallbackInfo")
  public PgPaymentInfoResponse getPaymentInfo(String transactionKey) {
    try {
      PgPaymentInfoResponse response = feignPgClient.getPaymentInfo(CUSTOMER_USER_ID, transactionKey).data();
      paymentMetricsService.recordPaymentResponse("/api/v1/payments/info", 200);
      return response;
    } catch (Exception e) {
      paymentMetricsService.recordPaymentException("/api/v1/payments/info", e);
      throw e;
    }
  }

  @Override
  @CircuitBreaker(name = "pgCircuit", fallbackMethod = "fallbackList")
  public PgPaymentListResponse getPaymentsByOrder(String orderId) {
    try {
      PgPaymentListResponse response = feignPgClient.getPaymentsByOrder(CUSTOMER_USER_ID, orderId).data();
      paymentMetricsService.recordPaymentResponse("/api/v1/payments/list", 200);
      return response;
    } catch (Exception e) {
      paymentMetricsService.recordPaymentException("/api/v1/payments/list", e);
      throw e;
    }
  }

  // fallback methods
  public PgPayResponse fallbackRequest(String orderId, CardType cardType, String cardNo, Money price, Throwable t) {
    return new PgPayResponse(null, "PENDING", t.getMessage());
  }

  public PgPaymentInfoResponse fallbackInfo(String transactionKey, Throwable t) {
    return new PgPaymentInfoResponse(transactionKey, null, null, "PENDING");
  }

  public PgPaymentListResponse fallbackList(String orderId, Throwable t) {
    return new PgPaymentListResponse(Collections.emptyList());
  }

}

