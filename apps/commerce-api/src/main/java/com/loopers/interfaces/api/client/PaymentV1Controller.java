package com.loopers.interfaces.api.client;

import com.loopers.application.payment.CreatePaymentCommand;
import com.loopers.application.payment.PaymentFacade;
import com.loopers.application.payment.PaymentInfo;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentV1Controller {

  private final PaymentFacade paymentFacade;

  @PostMapping
  public ApiResponse<PaymentInfo> requestPayment(@RequestBody PaymentCreateV1Dto.PaymentRequest dto) {
    CreatePaymentCommand command = CreatePaymentCommand.from(1L, dto); // userId는 실제로는 인증에서 가져와야 함
    PaymentInfo result = paymentFacade.requestPayment(command);
    return ApiResponse.success(result);
  }

  @PostMapping("/callback")
  public ApiResponse<PaymentCallbackV1Dto.CallbackResponse> handlePaymentCallback(
      @RequestBody PaymentCallbackV1Dto.CallbackRequest dto) {
    try {
      paymentFacade.handlePaymentCallback(dto);
      return ApiResponse.success(PaymentCallbackV1Dto.CallbackResponse.success());
    } catch (Exception e) {
      return ApiResponse.success(PaymentCallbackV1Dto.CallbackResponse.failure(e.getMessage()));
    }
  }

  @GetMapping("/{paymentId}")
  public ApiResponse<PaymentInfo> getPayment(@PathVariable String paymentId) {
    PaymentInfo result = paymentFacade.getPayment(paymentId);
    return ApiResponse.success(result);
  }

}
