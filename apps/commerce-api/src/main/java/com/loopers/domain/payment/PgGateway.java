package com.loopers.domain.payment;

import com.loopers.infrastructure.payment.client.dto.PgPaymentRequest;
import com.loopers.infrastructure.payment.client.dto.PgPaymentResponse;

/**
 * PG(Payment Gateway) 인터페이스
 * <p>
 * Domain Layer가 Infrastructure Layer에 직접 의존하지 않도록
 * 인터페이스를 Domain Layer에 정의합니다.
 * <p>
 * 실제 구현체(PgClient)는 Infrastructure Layer에 위치합니다.
 *
 * @author hyunjikoh
 * @since 2025. 12. 5.
 */
public interface PgGateway {

    /**
     * PG에 결제 요청
     *
     * @param username 사용자명
     * @param request  결제 요청 데이터
     * @return PG 응답
     */
    PgPaymentResponse requestPayment(String username, PgPaymentRequest request);

    /**
     * PG에서 결제 정보 조회
     *
     * @param username       사용자명
     * @param transactionKey 거래 키
     * @return PG 응답
     */
    PgPaymentResponse getPayment(String username, String transactionKey);
}

