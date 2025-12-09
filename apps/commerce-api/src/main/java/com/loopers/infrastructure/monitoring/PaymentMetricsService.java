package com.loopers.infrastructure.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentMetricsService {

  private final MeterRegistry meterRegistry;

  public void recordPaymentRequest(String apiEndpoint) {
    Counter.builder("payment.request")
        .description("Payment API request count")
        .tag("endpoint", apiEndpoint)
        .register(meterRegistry)
        .increment();
  }

  public void recordPaymentRequest(String apiEndpoint, String cardType) {
    Counter.builder("payment.request")
        .description("Payment API request count")
        .tag("endpoint", apiEndpoint)
        .tag("card_type", cardType != null ? cardType : "unknown")
        .register(meterRegistry)
        .increment();
  }

  public void recordPaymentResponse(String apiEndpoint, Integer statusCode) {
    Counter.builder("payment.response")
        .description("Payment API response count")
        .tag("endpoint", apiEndpoint)
        .tag("status_code", String.valueOf(statusCode))
        .tag("success", statusCode != null && statusCode < 400 ? "true" : "false")
        .register(meterRegistry)
        .increment();
  }

  public void recordPaymentResponse(String apiEndpoint, String statusCode, String cardType) {
    Counter.builder("payment.response")
        .description("Payment API response count")
        .tag("endpoint", apiEndpoint)
        .tag("status_code", String.valueOf(statusCode))
        .tag("card_type", cardType != null ? cardType : "unknown")
        .tag("success", statusCode != null && statusCode.equals("FAILED") ? "false" : "true")
        .register(meterRegistry)
        .increment();
  }

  public void recordPaymentCallback(boolean success) {
    Counter.builder("payment.callback")
        .description("Payment callback count")
        .tag("success", String.valueOf(success))
        .register(meterRegistry)
        .increment();
  }

  public void recordPaymentCallback(boolean success, String cardType) {
    Counter.builder("payment.callback")
        .description("Payment callback count")
        .tag("card_type", cardType != null ? cardType : "unknown")
        .tag("success", String.valueOf(success))
        .register(meterRegistry)
        .increment();
  }

  public void recordPaymentError(String apiEndpoint, String errorType) {
    Counter.builder("payment.error")
        .description("Payment error count")
        .tag("endpoint", apiEndpoint)
        .tag("error_type", errorType != null ? errorType : "unknown")
        .register(meterRegistry)
        .increment();
  }

  public void recordPaymentError(String apiEndpoint, String errorType, String cardType) {
    Counter.builder("payment.error")
        .description("Payment error count")
        .tag("endpoint", apiEndpoint)
        .tag("error_type", errorType != null ? errorType : "unknown")
        .tag("card_type", cardType != null ? cardType : "unknown")
        .register(meterRegistry)
        .increment();
  }

  public Timer.Sample startPaymentTimer(String operation) {
    Timer timer = Timer.builder("payment.duration")
        .description("Payment operation duration")
        .tag("operation", operation)
        .register(meterRegistry);
    return Timer.start(meterRegistry);
  }

  public void stopPaymentTimer(Timer.Sample sample, String operation, boolean success) {
    sample.stop(Timer.builder("payment.duration")
        .description("Payment operation duration")
        .tag("operation", operation)
        .tag("success", String.valueOf(success))
        .register(meterRegistry));
  }

  public void recordPaymentException(String apiEndpoint, Exception e, String cardType) {
    String errorType = e.getClass().getSimpleName();
    recordPaymentError(apiEndpoint, errorType, cardType);
  }

  public void recordPaymentException(String apiEndpoint, Exception e) {
    recordPaymentException(apiEndpoint, e, null);
  }
}
