package com.loopers.client.payment;

import com.loopers.client.payment.dto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * paymentgatewayservice 단위 테스트
 * feign 클라이언트를 mock으로 처리하여 비즈니스 로직 검증
 */
@ExtendWith(MockitoExtension.class)
class PaymentGatewayServiceTest {

    @Mock
    private PaymentGatewayClient paymentGatewayClient;

    @InjectMocks
    private PaymentGatewayService paymentGatewayService;

    @Test
    @DisplayName("결제 요청 성공 케이스")
    void requestPayment_Success() {
        // arrange - 테스트 데이터 및 mock 설정
        String userId = "test-user-001";
        PaymentV1Dto.Request request = PaymentV1Dto.Request.of(
                        "ORDER123456",
                        "SAMSUNG",
                        "1234-5678-9999-0000",
                new BigDecimal("10000"),
                        "http://localhost:8080/api/v1/callback"
                );

        PaymentV1Dto.Response expectedResponse = PaymentV1Dto.Response.of(
                "20250821:TR:abc123",
                "PENDING",
                "결제 요청이 접수되었습니다."
        );

        ApiResponse<PaymentV1Dto.Response> mockApiResponse = ApiResponse.success(expectedResponse);
        when(paymentGatewayClient.requestPayment(eq(userId), eq(request)))
                .thenReturn(mockApiResponse);

        // act - 테스트 대상 메서드 실행
        PaymentV1Dto.Response actualResponse = paymentGatewayService.requestPayment(userId, request);

        // assert - 결과 검증
        assertNotNull(actualResponse);
        assertEquals(expectedResponse.transactionKey(), actualResponse.transactionKey());
        assertEquals(expectedResponse.status(), actualResponse.status());
        assertTrue(actualResponse.isPending());

        verify(paymentGatewayClient, times(1)).requestPayment(userId, request);
    }

    @Test
    @DisplayName("결제 요청 실패 - API 응답 실패")
    void requestPayment_ApiFailure() {
        // arrange - 실패 케이스 테스트 데이터 및 mock 설정
        String userId = "test-user-001";
        PaymentV1Dto.Request request = PaymentV1Dto.Request.of(
                "INVALID",
                "SAMSUNG",
                "1234-5678-9999-0000",
                new BigDecimal("10000"),
                "http://localhost:8080/api/v1/callback"
        );

        ApiResponse<PaymentV1Dto.Response> mockErrorResponse =
                ApiResponse.failure("주문 ID는 6자리 이상 문자열이어야 합니다.");

        when(paymentGatewayClient.requestPayment(eq(userId), eq(request)))
                .thenReturn(mockErrorResponse);

        // act - 예외 발생 메서드 실행 및 예외 검증
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> paymentGatewayService.requestPayment(userId, request)
        );

        // assert - 예외 메시지 및 mock 호출 검증
        assertTrue(exception.getMessage().contains("결제 요청이 실패했습니다") ||
                exception.getCause().getMessage().contains("결제 요청이 실패했습니다"));
        verify(paymentGatewayClient, times(1)).requestPayment(userId, request);
    }

    @Test
    @DisplayName("결제 상세 조회 성공")
    void getPaymentDetail_Success() {
        // arrange - 결제 상세 조회 성공 케이스 테스트 데이터 설정
        String userId = "test-user-001";
        String transactionKey = "20250821:TR:abc123";

        PaymentV1Dto.Response.Detail expectedDetail =
                PaymentV1Dto.Response.Detail.of(
                        transactionKey,
                        "ORDER123456",
                        "SAMSUNG",
                        "1234-5678-****-****",
                        new BigDecimal("10000"),
                        "SUCCESS",
                        "정상 승인되었습니다."
                );

        ApiResponse<PaymentV1Dto.Response.Detail> mockApiResponse = ApiResponse.success(expectedDetail);
        when(paymentGatewayClient.getPaymentDetail(userId, transactionKey))
                .thenReturn(mockApiResponse);

        // act - 결제 상세 조회 메서드 실행
        PaymentV1Dto.Response.Detail actualDetail = paymentGatewayService.getPaymentDetail(userId, transactionKey);

        // assert - 결제 상세 정보 검증
        assertNotNull(actualDetail);
        assertEquals(expectedDetail.transactionKey(), actualDetail.transactionKey());
        assertEquals(expectedDetail.orderId(), actualDetail.orderId());
        assertEquals(expectedDetail.amount(), actualDetail.amount());
        assertTrue(actualDetail.isSuccess());

        verify(paymentGatewayClient, times(1)).getPaymentDetail(userId, transactionKey);
    }

    @Test
    @DisplayName("결제 상세 조회 실패 - 응답 데이터 없음")
    void getPaymentDetail_NoData() {
        // arrange - 존재하지 않는 거래키로 null 응답 설정
        String userId = "test-user-001";
        String transactionKey = "NOT-EXIST-KEY";

        ApiResponse<PaymentV1Dto.Response.Detail> mockApiResponse = ApiResponse.success(null);
        when(paymentGatewayClient.getPaymentDetail(userId, transactionKey))
                .thenReturn(mockApiResponse);

        // act - 존재하지 않는 결제 정보 조회 시 예외 발생 검증
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> paymentGatewayService.getPaymentDetail(userId, transactionKey)
        );

        // assert - 예외 메시지 및 mock 호출 검증
        assertTrue(exception.getMessage().contains("결제 정보를 찾을 수 없습니다") ||
                exception.getCause().getMessage().contains("결제 정보를 찾을 수 없습니다"));
        verify(paymentGatewayClient, times(1)).getPaymentDetail(userId, transactionKey);
    }

    @Test
    @DisplayName("주문별 결제 내역 조회 성공")
    void getPaymentsByOrderId_Success() {
        // arrange - 주문별 결제 내역 조회 테스트 데이터 설정
        String userId = "test-user-001";
        String orderId = "ORDER123456";

        Object mockPaymentHistory = new Object(); // 실제 응답 객체
        ApiResponse<Object> mockApiResponse = ApiResponse.success(mockPaymentHistory);

        when(paymentGatewayClient.getPaymentsByOrderId(userId, orderId))
                .thenReturn(mockApiResponse);

        // act - 주문별 결제 내역 조회 메서드 실행
        Object result = paymentGatewayService.getPaymentsByOrderId(userId, orderId);

        // assert - 결제 내역 조회 결과 검증
        assertNotNull(result);
        assertEquals(mockPaymentHistory, result);

        verify(paymentGatewayClient, times(1)).getPaymentsByOrderId(userId, orderId);
    }

    @Test
    @DisplayName("주문별 결제 내역 조회 - 데이터 없음")
    void getPaymentsByOrderId_NoData() {
        // arrange - 결제 내역이 없는 주문 테스트 데이터 설정
        String userId = "test-user-001";
        String orderId = "NO-PAYMENT-ORDER";

        ApiResponse<Object> mockApiResponse = ApiResponse.success(null);
        when(paymentGatewayClient.getPaymentsByOrderId(userId, orderId))
                .thenReturn(mockApiResponse);

        // act - 결제 내역이 없는 주문 조회 메서드 실행
        Object result = paymentGatewayService.getPaymentsByOrderId(userId, orderId);

        // assert - null 결과 검증
        assertNull(result);
        verify(paymentGatewayClient, times(1)).getPaymentsByOrderId(userId, orderId);
    }

    @Test
    @DisplayName("결제 완료 여부 확인 - 성공 상태")
    void isPaymentCompleted_Success() {
        // arrange - 성공 상태 결제 정보 설정
        String userId = "test-user-001";
        String transactionKey = "20250821:TR:abc123";

        PaymentV1Dto.Response.Detail completedPayment =
                PaymentV1Dto.Response.Detail.of(
                        transactionKey,
                        null,
                        null,
                        null,
                        new BigDecimal(0),
                        "SUCCESS",
                        null
                );

        ApiResponse<PaymentV1Dto.Response.Detail> mockApiResponse = ApiResponse.success(completedPayment);
        when(paymentGatewayClient.getPaymentDetail(userId, transactionKey))
                .thenReturn(mockApiResponse);

        // act - 결제 완료 여부 확인 메서드 실행
        boolean isCompleted = paymentGatewayService.isPaymentCompleted(userId, transactionKey);

        // assert - 성공 상태 완료 여부 검증
        assertTrue(isCompleted);
        verify(paymentGatewayClient, times(1)).getPaymentDetail(userId, transactionKey);
    }

    @Test
    @DisplayName("결제 완료 여부 확인 - 실패 상태")
    void isPaymentCompleted_Failed() {
        // arrange - 실패 상태 결제 정보 설정
        String userId = "test-user-001";
        String transactionKey = "20250821:TR:abc123";

        PaymentV1Dto.Response.Detail failedPayment =
                PaymentV1Dto.Response.Detail.of(
                        transactionKey,
                        null,
                        null,
                        null,
                        new BigDecimal(0),
                        "FAILED",
                        null
                );

        ApiResponse<PaymentV1Dto.Response.Detail> mockApiResponse = ApiResponse.success(failedPayment);
        when(paymentGatewayClient.getPaymentDetail(userId, transactionKey))
                .thenReturn(mockApiResponse);

        // act - 결제 완료 여부 확인 메서드 실행
        boolean isCompleted = paymentGatewayService.isPaymentCompleted(userId, transactionKey);

        // assert - 실패 상태도 완료로 간주하는지 검증
        assertTrue(isCompleted); // failed도 완료된 상태
        verify(paymentGatewayClient, times(1)).getPaymentDetail(userId, transactionKey);
    }

    @Test
    @DisplayName("결제 완료 여부 확인 - 대기 상태")
    void isPaymentCompleted_Pending() {
        // arrange - 대기 상태 결제 정보 설정
        String userId = "test-user-001";
        String transactionKey = "20250821:TR:abc123";

        PaymentV1Dto.Response.Detail pendingPayment =
                PaymentV1Dto.Response.Detail.of(
                        transactionKey,
                        null,
                        null,
                        null,
                        new BigDecimal(0),
                        "PENDING",
                        null
                );

        ApiResponse<PaymentV1Dto.Response.Detail> mockApiResponse = ApiResponse.success(pendingPayment);
        when(paymentGatewayClient.getPaymentDetail(userId, transactionKey))
                .thenReturn(mockApiResponse);

        // act - 결제 완료 여부 확인 메서드 실행
        boolean isCompleted = paymentGatewayService.isPaymentCompleted(userId, transactionKey);

        // assert - 대기 상태는 미완료로 검증
        assertFalse(isCompleted);
        verify(paymentGatewayClient, times(1)).getPaymentDetail(userId, transactionKey);
    }

    @Test
    @DisplayName("결제 완료 여부 확인 - 예외 발생 시 false 반환")
    void isPaymentCompleted_Exception() {
        // arrange - 네트워크 오류 예외 발생 설정
        String userId = "test-user-001";
        String transactionKey = "20250821:TR:abc123";

        when(paymentGatewayClient.getPaymentDetail(userId, transactionKey))
                .thenThrow(new RuntimeException("네트워크 오류"));

        // act - 예외 발생 시 결제 완료 여부 확인 메서드 실행
        boolean isCompleted = paymentGatewayService.isPaymentCompleted(userId, transactionKey);

        // assert - 예외 발생 시 false 반환 검증
        assertFalse(isCompleted);
        verify(paymentGatewayClient, times(1)).getPaymentDetail(userId, transactionKey);
    }
}
