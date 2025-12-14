package com.loopers.application.payment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.payment.PaymentEntity;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 결제 타임아웃 처리 서비스
 * <p>
 * 스케줄러로부터 호출되어 개별 결제 건의 타임아웃을 처리합니다.
 * <p>
 * 트랜잭션 전파 전략: REQUIRES_NEW
 * - 각 결제 건마다 새로운 독립적인 트랜잭션 생성
 * - 한 건 실패해도 다른 건에 영향 없음
 * - 짧은 트랜잭션으로 DB 락 경합 최소화
 *
 * @author hyunjikoh
 * @since 2025. 12. 5.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentTimeoutProcessor {

    /**
     * 개별 결제 타임아웃 처리
     *
     * @param payment 타임아웃 처리할 결제
     * @return 처리 성공 여부
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean processTimeout(PaymentEntity payment) {
        try {
            log.warn("결제 타임아웃 처리 - transactionKey: {}, orderNumber: {}, requestedAt: {}",
                    payment.getTransactionKey(),
                    payment.getOrderNumber(),
                    payment.getRequestedAt());

            payment.timeoutWithEvent();

            return true;
        } catch (Exception e) {
            log.error("결제 타임아웃 처리 실패 - transactionKey: {}, orderNumber: {}",
                    payment.getTransactionKey(), payment.getOrderNumber(), e);
            return false;
        }
    }
}

