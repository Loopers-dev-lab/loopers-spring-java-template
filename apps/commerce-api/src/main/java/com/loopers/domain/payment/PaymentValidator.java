package com.loopers.domain.payment;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.loopers.domain.order.OrderEntity;
import com.loopers.domain.order.OrderStatus;
import com.loopers.infrastructure.payment.client.dto.PgPaymentResponse;
import com.loopers.interfaces.api.payment.PaymentV1Dtos;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import lombok.extern.slf4j.Slf4j;

/**
 * 결제 도메인 검증 로직
 * DDD 원칙에 따라 비즈니스 검증 로직을 도메인 계층에 위치시킵니다.
 *
 * @author hyunjikoh
 * @since 2025. 12. 05.
 */
@Component
@Slf4j
public class PaymentValidator {

    /**
     * 주문 결제 가능 여부 검증
     * 검증 항목:
     * - 주문 상태 확인 (PENDING만 결제 가능)
     * - 결제 금액 일치 여부
     *
     * @param order         주문 엔티티
     * @param paymentAmount 결제 금액
     * @throws IllegalArgumentException 검증 실패 시
     */
    public void validateOrderForPayment(OrderEntity order, BigDecimal paymentAmount) {
        // 주문 상태 확인 (PENDING 상태만 결제 가능)
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException(
                    String.format("결제 가능한 주문 상태가 아닙니다. (현재 상태: %s)", order.getStatus())
            );
        }

        // 결제 금액 정합성 검증
        if (order.getFinalTotalAmount().compareTo(paymentAmount) != 0) {
            throw new IllegalArgumentException(
                    String.format("결제 금액이 주문 금액과 일치하지 않습니다. (주문 금액: %s, 결제 금액: %s)",
                            order.getFinalTotalAmount(), paymentAmount)
            );
        }

        log.debug("주문 결제 검증 완료 - orderNumber: {}, amount: {}", order.getId(), paymentAmount);
    }

    /**
     * PG 콜백 데이터 검증
     * <p>
     * 검증 항목:
     * - 주문 ID 일치 여부
     * - 콜백 상태 vs PG 실제 상태 일치 여부
     *
     * @param payment         결제 엔티티
     * @param callbackRequest 콜백 요청 데이터
     * @param pgData          PG 실제 데이터
     */
    public void validateCallbackData(
            PaymentEntity payment,
            PaymentV1Dtos.PgCallbackRequest callbackRequest,
            PgPaymentResponse pgData
    ) {
        // 주문 ID 검증
        Long callbackOrderId = Long.parseLong(callbackRequest.orderId());
        if (!payment.getOrderNumber().equals(callbackOrderId)) {
            throw new IllegalArgumentException(
                    String.format("주문 ID 불일치 - DB: %d, 콜백: %d",
                            payment.getOrderNumber(), callbackOrderId)
            );
        }

        // 콜백 상태 vs PG 실제 상태 검증
        if (!callbackRequest.status().equals(pgData.status())) {
            log.warn("콜백 상태와 PG 실제 상태 불일치 - 콜백: {}, PG: {}",
                    callbackRequest.status(), pgData.status());
            throw new IllegalArgumentException(
                    String.format("결제 상태 불일치 - 콜백: %s, PG: %s",
                            callbackRequest.status(), pgData.status())
            );
        }

        log.debug("콜백 데이터 검증 완료 - transactionKey: {}", callbackRequest.transactionKey());
    }

    /**
     * 주문 금액 vs 결제 금액 검증
     * <p>
     * 주문 금액과 PG 결제 금액이 일치하는지 검증
     *
     * @param order    주문 엔티티
     * @param pgAmount PG 결제 금액
     */
    public void validateOrderAmount(OrderEntity order, BigDecimal pgAmount) {
        if (order.getFinalTotalAmount().compareTo(pgAmount) != 0) {
            throw new IllegalArgumentException(
                    String.format("주문 금액과 결제 금액 불일치 - 주문: %s, PG: %s",
                            order.getFinalTotalAmount(), pgAmount)
            );
        }

        log.debug("주문 금액 검증 완료 - orderNumber: {}, amount: {}", order.getId(), pgAmount);
    }

    /**
     * PG API 응답 검증
     *
     * @param pgResponse PG 응답
     * @throws RuntimeException API 실패 시
     */
    public void validatePgResponse(PgPaymentResponse pgResponse) {
        if (pgResponse.isApiFail()) {
            String errorMessage = String.format("PG 결제 요청 실패 - errorCode: %s, message: %s",
                    pgResponse.meta().errorCode(),
                    pgResponse.meta().message());
            log.error(errorMessage);
            throw new CoreException(ErrorType.PG_API_FAIL, errorMessage);
        }

        if (pgResponse.data() == null || pgResponse.transactionKey() == null) {
            throw new CoreException(ErrorType.INVALID_PG_RESPONSE, "PG 응답에 transactionKey가 없습니다.");
        }
    }
}

