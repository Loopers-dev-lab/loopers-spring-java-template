package com.loopers.infrastructure.payment.client;

import java.util.List;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import com.loopers.infrastructure.payment.client.dto.PgPaymentRequest;
import com.loopers.infrastructure.payment.client.dto.PgPaymentResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * @author hyunjikoh
 * @since 2025. 12. 3.
 */
// 2. Fallback Factory 구현
@Slf4j
@Component
public class PgClientFallbackFactory implements FallbackFactory<PgClient> {

    @Override
    public PgClient create(Throwable cause) {
        return new PgClient() {

            @Override
            public PgPaymentResponse requestPayment(String userId, PgPaymentRequest request) {
                log.error("PG 결제 요청 실패 - userId: {}, orderNumber: {}",
                        userId, request.orderId(), cause);

                // 장애 상황에 대한 처리
                throw new PgServiceUnavailableException(
                        "결제 시스템이 일시적으로 사용 불가능합니다.", cause);
            }

            @Override
            public PgPaymentResponse getPayment(String userId, String transactionKey) {
                log.error("PG 결제 조회 실패 - userId: {}, transactionKey: {}",
                        userId, transactionKey, cause);

                throw new PgServiceUnavailableException(
                        "결제 정보 조회가 일시적으로 불가능합니다.", cause);
            }

            @Override
            public List<PgPaymentResponse> getPaymentsByOrderId(String userId, String orderId) {
                log.error("PG 결제 조회 실패 - userId: {}, orderNumber: {}",
                        userId, orderId, cause);

                throw new PgServiceUnavailableException(
                        "결제 정보 조회가 일시적으로 불가능합니다.", cause);
            }
        };
    }
}

