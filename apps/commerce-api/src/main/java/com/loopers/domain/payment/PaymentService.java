package com.loopers.domain.payment;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    /**
     * Payment 저장
     */
    public Payment save(Payment payment) {
        return paymentRepository.save(payment);
    }

    /**
     * TransactionKey로 Payment 조회
     *
     * @param transactionKey PG 거래 키
     * @return Payment 엔티티
     * @throws CoreException 결제 정보를 찾을 수 없는 경우
     */
    public Payment getPaymentByTransactionKey(String transactionKey) {
        return paymentRepository.findByPgTransactionId(transactionKey)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND,
                        "결제 정보를 찾을 수 없습니다: " + transactionKey
                ));
    }
}
