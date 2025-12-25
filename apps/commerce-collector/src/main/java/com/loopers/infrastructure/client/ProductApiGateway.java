package com.loopers.infrastructure.client;

import com.loopers.infrastructure.client.dto.ProductDetailExternalDto;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductApiGateway {

    private final ProductApiClient productApiClient;

    /**
     * 상품 상세 조회 (CircuitBreaker + Retry 적용)
     *
     * @param productId 상품 ID
     * @return 상품 상세 정보
     */
    @Retry(name = "productApi")
    @CircuitBreaker(name = "productApi", fallbackMethod = "getProductDetailFallback")
    public ProductDetailExternalDto.ProductDetailResponse getProductDetail(Long productId) {
        log.debug("상품 상세 조회 요청 - productId: {}", productId);

        try {
            ProductDetailExternalDto.ProductDetailResponse response = productApiClient.getProductDetail(productId);
            log.debug("상품 상세 조회 성공 - productId: {}, stockQuantity: {}",
                    productId, response.getStockQuantity());
            return response;
        } catch (FeignException.NotFound e) {
            log.error("상품을 찾을 수 없음 - productId: {}", productId, e);
            throw new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다");
        } catch (FeignException e) {
            log.error("상품 정보 조회 실패 - productId: {}, status: {}",
                    productId, e.status(), e);
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 정보 조회 실패");
        }
    }

    /**
     * Fallback 메서드
     * Circuit이 Open 상태이거나, 재시도 실패 시 호출
     *
     * @param productId 상품 ID
     * @param ex 발생한 예외
     * @return 기본 응답 (높은 재고값으로 캐시 무효화 방지)
     */
    private ProductDetailExternalDto.ProductDetailResponse getProductDetailFallback(
            Long productId, Exception ex) {
        log.error("상품 API 시스템 장애 발생 - productId: {}, error: {}",
                productId, ex.getMessage(), ex);

        // Circuit이 Open이거나 재시도 실패 시
        // 재고 임계값 체크를 건너뛰기 위해 높은 재고값 반환
        // 실제로는 캐시 무효화가 발생하지 않음
        return new ProductDetailExternalDto.ProductDetailResponse(
                productId,
                null,  // productCode
                null,  // productName
                null,  // price
                Integer.MAX_VALUE,  // stock
                null,  // likeCount
                null   // brand
        );
    }
}