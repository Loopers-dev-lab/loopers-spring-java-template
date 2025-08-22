package com.loopers.client.payment;

import com.loopers.client.payment.dto.ApiResponse;
import com.loopers.client.payment.dto.PaymentV1Dto;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentGatewayService {

    private final PaymentGatewayClient paymentGatewayClient;
    @CircuitBreaker(name = "payment-gateway", fallbackMethod = "paymentFallback")
    public PaymentV1Dto.Response requestPayment(String userId, PaymentV1Dto.Request request) {
        log.info("결제 요청 시작 - UserId: {}, OrderId: {}, Amount: {}",
                userId, request.orderId(), request.amount());
        try {
            ApiResponse<PaymentV1Dto.Response> apiResponse = paymentGatewayClient.requestPayment(userId, request);

            if (!apiResponse.isSuccess()) {
                log.error("결제 요청 실패 - UserId: {}, OrderId: {}, Error: {}",
                        userId, request.orderId(), apiResponse.getErrorMessage());
                throw new IllegalStateException("결제 요청이 실패했습니다: " + apiResponse.getErrorMessage());
            }

            if (!apiResponse.hasData()) {
                log.error("결제 응답 데이터 없음 - UserId: {}, OrderId: {}", userId, request.orderId());
                throw new IllegalStateException("결제 응답에서 데이터를 찾을 수 없습니다.");
            }

            PaymentV1Dto.Response paymentResponse = apiResponse.getData();
            log.info("결제 요청 완료 - UserId: {}, OrderId: {}, TransactionKey: {}, Status: {}",
                    userId, request.orderId(), paymentResponse.transactionKey(), paymentResponse.status());

            return paymentResponse;

        } catch (Exception e) {
            log.error("결제 요청 예상치 못한 오류 - UserId: {}, OrderId: {}, Error: {}",
                    userId, request.orderId(), e.getMessage(), e);
            throw new CoreException(ErrorType.INTERNAL_ERROR, e.getMessage());
        }
    }


    public PaymentV1Dto.Response.Detail getPaymentDetail(String userId, String transactionKey) {
        log.info("결제 상세 조회 시작 - UserId: {}, TransactionKey: {}", userId, transactionKey);

        try {
            ApiResponse<PaymentV1Dto.Response.Detail> apiResponse =
                    paymentGatewayClient.getPaymentDetail(userId, transactionKey);

            if (!apiResponse.isSuccess()) {
                log.error("결제 상세 조회 실패 - UserId: {}, TransactionKey: {}, Error: {}",
                        userId, transactionKey, apiResponse.getErrorMessage());
                throw new IllegalStateException("결제 정보 조회가 실패했습니다: " + apiResponse.getErrorMessage());
            }

            if (!apiResponse.hasData()) {
                log.error("결제 상세 응답 데이터 없음 - UserId: {}, TransactionKey: {}", userId, transactionKey);
                throw new IllegalStateException("결제 정보를 찾을 수 없습니다.");
            }

            PaymentV1Dto.Response.Detail detailResponse = apiResponse.getData();
            log.info("결제 상세 조회 완료 - UserId: {}, TransactionKey: {}, Status: {}, Amount: {}",
                    userId, transactionKey, detailResponse.status(), detailResponse.amount());

            return detailResponse;

        } catch (Exception e) {
            log.error("결제 상세 조회 예상치 못한 오류 - UserId: {}, TransactionKey: {}, Error: {}",
                    userId, transactionKey, e.getMessage(), e);
            throw new RuntimeException("결제 정보 조회 중 오류가 발생했습니다.", e);
        }
    }

    public Object getPaymentsByOrderId(String userId, String orderId) {
        log.info("주문별 결제 내역 조회 시작 - UserId: {}, OrderId: {}", userId, orderId);

        try {
            ApiResponse<Object> apiResponse = paymentGatewayClient.getPaymentsByOrderId(userId, orderId);

            if (!apiResponse.isSuccess()) {
                log.error("주문별 결제 내역 조회 실패 - UserId: {}, OrderId: {}, Error: {}",
                        userId, orderId, apiResponse.getErrorMessage());
                throw new IllegalStateException("주문별 결제 내역 조회가 실패했습니다: " + apiResponse.getErrorMessage());
            }

            if (!apiResponse.hasData()) {
                log.warn("주문별 결제 내역 없음 - UserId: {}, OrderId: {}", userId, orderId);
                return null; // 데이터가 없는 것은 정상적인 상황일 수 있음
            }

            Object paymentHistory = apiResponse.getData();
            log.info("주문별 결제 내역 조회 완료 - UserId: {}, OrderId: {}", userId, orderId);

            return paymentHistory;

        }catch (Exception e) {
            log.error("주문별 결제 내역 조회 예상치 못한 오류 - UserId: {}, OrderId: {}, Error: {}",
                    userId, orderId, e.getMessage(), e);
            throw new RuntimeException("주문별 결제 내역 조회 중 오류가 발생했습니다.", e);
        }
    }

    public boolean isPaymentCompleted(String userId, String transactionKey) {
        try {
            PaymentV1Dto.Response.Detail detail = getPaymentDetail(userId, transactionKey);
            return detail.isSuccess() || detail.isFailed();
        } catch (Exception e) {
            log.warn("결제 완료 확인 중 오류 - UserId: {}, TransactionKey: {}, Error: {}",
                    userId, transactionKey, e.getMessage());
            return false;
        }
    }
}
