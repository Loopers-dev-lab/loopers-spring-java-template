package com.loopers.infrastructure.client;

import com.loopers.infrastructure.client.dto.ProductDetailExternalDto;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * ProductApiGateway 단위 테스트
 * Circuit Breaker 동작을 검증하는 테스트
 */
@ExtendWith(MockitoExtension.class)
class ProductApiGatewayTest {

    private static final Long TEST_PRODUCT_ID = 1L;
    private static final int TEST_STOCK_QUANTITY = 50;
    private static final String CIRCUIT_BREAKER_NAME = "productApi";

    // Circuit Breaker 설정 상수 (application.yml과 동일)
    private static final int SLIDING_WINDOW_SIZE = 10;
    private static final int MINIMUM_NUMBER_OF_CALLS = 5;
    private static final float FAILURE_RATE_THRESHOLD = 50f;
    private static final int WAIT_DURATION_SECONDS = 10;
    private static final int PERMITTED_CALLS_IN_HALF_OPEN = 2;

    private ProductApiGateway productApiGateway;
    private CircuitBreaker circuitBreaker;

    @Mock
    private ProductApiClient productApiClient;

    @BeforeEach
    void setUp() {
        circuitBreaker = createCircuitBreaker();
        productApiGateway = new ProductApiGateway(productApiClient);
    }

    @DisplayName("정상 응답 시 상품 상세 정보를 반환한다")
    @Test
    void shouldReturnProductDetailWhenSuccess() {
        // given
        ProductDetailExternalDto.ProductDetailResponse expectedResponse =
                createProductDetailResponse(TEST_PRODUCT_ID, TEST_STOCK_QUANTITY);

        given(productApiClient.getProductDetail(TEST_PRODUCT_ID))
                .willReturn(expectedResponse);

        // when
        ProductDetailExternalDto.ProductDetailResponse response =
                productApiGateway.getProductDetail(TEST_PRODUCT_ID);

        // then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(TEST_PRODUCT_ID);
        assertThat(response.getStockQuantity()).isEqualTo(TEST_STOCK_QUANTITY);
    }

    @DisplayName("상품을 찾을 수 없을 때 CoreException(NOT_FOUND)을 던진다")
    @Test
    void shouldThrowCoreExceptionWhenProductNotFound() {
        // given
        given(productApiClient.getProductDetail(TEST_PRODUCT_ID))
                .willThrow(FeignException.NotFound.class);

        // when & then
        assertThatThrownBy(() -> productApiGateway.getProductDetail(TEST_PRODUCT_ID))
                .isInstanceOf(CoreException.class)
                .hasMessage("상품을 찾을 수 없습니다")
                .extracting("errorType")
                .isEqualTo(ErrorType.NOT_FOUND);
    }

    @DisplayName("Feign 예외 발생 시 CoreException(BAD_REQUEST)을 던진다")
    @Test
    void shouldThrowCoreExceptionWhenFeignException() {
        // given
        given(productApiClient.getProductDetail(TEST_PRODUCT_ID))
                .willThrow(FeignException.InternalServerError.class);

        // when & then
        assertThatThrownBy(() -> productApiGateway.getProductDetail(TEST_PRODUCT_ID))
                .isInstanceOf(CoreException.class)
                .hasMessage("상품 정보 조회 실패")
                .extracting("errorType")
                .isEqualTo(ErrorType.BAD_REQUEST);
    }

    @DisplayName("연속 실패 시 Circuit Breaker가 OPEN 상태로 전환된다")
    @Test
    void shouldOpenCircuitBreakerAfterConsecutiveFailures() {
        // given
        given(productApiClient.getProductDetail(any()))
                .willThrow(new RuntimeException("Product API unavailable"));

        // when
        int failureCount = executeGetProductDetailAndCountFailures(MINIMUM_NUMBER_OF_CALLS);

        // then
        assertThat(failureCount).isEqualTo(MINIMUM_NUMBER_OF_CALLS);
        assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls())
                .isEqualTo(MINIMUM_NUMBER_OF_CALLS);
        assertThat(circuitBreaker.getMetrics().getFailureRate()).isEqualTo(100.0f);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    /**
     * Circuit Breaker 설정 생성 (application.yml과 동일한 설정)
     */
    private CircuitBreaker createCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(SLIDING_WINDOW_SIZE)
                .minimumNumberOfCalls(MINIMUM_NUMBER_OF_CALLS)
                .failureRateThreshold(FAILURE_RATE_THRESHOLD)
                .waitDurationInOpenState(Duration.ofSeconds(WAIT_DURATION_SECONDS))
                .permittedNumberOfCallsInHalfOpenState(PERMITTED_CALLS_IN_HALF_OPEN)
                .build();

        return CircuitBreaker.of(CIRCUIT_BREAKER_NAME, config);
    }

    /**
     * 상품 상세 조회를 여러 번 시도하고 실패 횟수를 반환
     */
    private int executeGetProductDetailAndCountFailures(int attempts) {
        int failureCount = 0;

        for (int i = 0; i < attempts; i++) {
            try {
                circuitBreaker.executeSupplier(() ->
                        productApiGateway.getProductDetail(TEST_PRODUCT_ID)
                );
            } catch (Exception e) {
                failureCount++;
            }
        }

        return failureCount;
    }

    /**
     * 테스트용 ProductDetailResponse 객체 생성
     */
    private ProductDetailExternalDto.ProductDetailResponse createProductDetailResponse(
            Long productId, int stockQuantity) {
        return new ProductDetailExternalDto.ProductDetailResponse(
                productId,
                "TEST-CODE",
                "Test Product",
                null,
                stockQuantity,
                0L,
                null
        );
    }
}