package com.loopers.infrastructure.scheduler;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentGateway;
import com.loopers.domain.payment.PaymentRepository;
import com.loopers.domain.payment.PaymentResult;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 상태 확인 서비스
 * - 각 결제를 독립적인 트랜잭션으로 처리하여 부분 실패 시에도 성공한 결제는 커밋되도록 함
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentStatusCheckService {

    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;

    /**
     * 개별 결제 상태 확인 (독립적인 트랜잭션)
     * - REQUIRES_NEW로 각 결제마다 새로운 트랜잭션 생성
     * - 한 결제 실패가 다른 결제에 영향 없음
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void checkPaymentStatus(String paymentId) {
        try {
            Payment payment = paymentRepository.findByPaymentId(paymentId)
                    .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "결제를 찾을 수 없습니다"));

            log.info("[결제 상태 확인] 결제 확인 중: paymentId={}, pgTransactionId={}, checkCount={}",
                    payment.getPaymentId(), payment.getPgTransactionId(), payment.getStatusCheckCount());

            // PG사에 상태 확인 요청
            PaymentResult result = paymentGateway.checkPaymentStatus(payment.getPgTransactionId());

            // 확인 횟수 증가
            payment.incrementStatusCheckCount();

            // 결과에 따라 결제 상태 업데이트
            if (result.isSuccess()) {
                payment.completePayment();
                log.info("[결제 상태 확인] 결제 완료: paymentId={}", payment.getPaymentId());
            } else if ("FAILED".equals(result.status()) || "FAIL".equals(result.status())) {
                payment.failPayment(result.message());
                log.warn("[결제 상태 확인] 결제 실패: paymentId={}, reason={}",
                        payment.getPaymentId(), result.message());
            } else {
                // PROCESSING 상태 유지
                log.info("[결제 상태 확인] 결제 처리 중: paymentId={}",
                        payment.getPaymentId());
            }

            paymentRepository.save(payment);

        } catch (Exception e) {
            log.error("[결제 상태 확인] 결제 상태 확인 중 오류 발생: paymentId={}", paymentId, e);

            // 에러 발생 시에도 확인 횟수는 증가
            try {
                Payment payment = paymentRepository.findByPaymentId(paymentId).orElse(null);
                if (payment != null) {
                    payment.incrementStatusCheckCount();
                    paymentRepository.save(payment);
                }
            } catch (Exception ex) {
                log.error("[결제 상태 확인] 확인 횟수 증가 중 오류 발생: paymentId={}", paymentId, ex);
            }
        }
    }
}
