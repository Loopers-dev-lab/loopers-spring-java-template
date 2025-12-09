package com.loopers.infrastructure.feign;

import com.loopers.application.payment.PgClient;
import com.loopers.application.payment.PgPayRequest;
import com.loopers.application.payment.PgPayResponse;
import com.loopers.application.payment.PgPaymentInfoResponse;
import com.loopers.application.payment.PgPaymentListResponse;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.infrastructure.monitoring.PaymentMetricsService;
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

  @Override
  @CircuitBreaker(name = "pgCircuit", fallbackMethod = "fallbackRequest")
  public PgPayResponse requestPayment(PgPayRequest request) {
    paymentMetricsService.recordPaymentRequest("/api/v1/payments", request.cardType());

    try {
      var apiResponse = feignPgClient.requestPayment(CUSTOMER_USER_ID, request);

      if (apiResponse.meta().result() != ApiResponse.Metadata.Result.SUCCESS) {
        String errorMessage = "PG 요청 실패: " + apiResponse.meta().message();
        paymentMetricsService.recordPaymentError("/api/v1/payments", "pg_error", request.cardType());
        throw new RuntimeException(errorMessage);
      }

      PgPayResponse response = apiResponse.data();
      paymentMetricsService.recordPaymentResponse("/api/v1/payments", response.status(), request.cardType());
      return response;
    } catch (Exception e) {
      paymentMetricsService.recordPaymentException("/api/v1/payments", e, request.cardType());
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
  public PgPayResponse fallbackRequest(PgPayRequest request, Throwable t) {
    return new PgPayResponse(null, "PENDING", t.getMessage());
  }

  public PgPaymentInfoResponse fallbackInfo(String transactionKey, Throwable t) {
    return new PgPaymentInfoResponse(transactionKey, null, null, "PENDING");
  }

  public PgPaymentListResponse fallbackList(String orderId, Throwable t) {
    return new PgPaymentListResponse(Collections.emptyList());
  }
}

