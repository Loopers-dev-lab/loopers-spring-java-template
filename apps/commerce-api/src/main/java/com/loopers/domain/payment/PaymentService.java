package com.loopers.domain.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

@Component
@RequiredArgsConstructor
public class PaymentService {

    private final CommercePaymentRepository commercePaymentRepository;

    /**
     * CommercePayment 저장
     */
    @Transactional
    public CommercePayment saveCommercePayment(CommercePayment commercePayment) {
        return commercePaymentRepository.save(commercePayment)
            .orElseThrow(() -> new CoreException(ErrorType.INTERNAL_ERROR, "CommercePayment 저장에 실패했습니다."));
    }

    /**
     * TransactionKey에 해당하는 CommercePayment 조회
     */
    @Transactional(readOnly = true)
    public CommercePayment findByTransactionKey(String transactionKey) {
        return commercePaymentRepository.findByTransactionKey(transactionKey)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "[transactionKey = " + transactionKey + "] CommercePayment를 찾을 수 없습니다."));
    }

    /**
     * 결제 실패 처리
     */
    @Transactional
    public void saveFailedPayment(String transactionKey, String reason) {
        CommercePayment foundCommercePayment = findByTransactionKey(transactionKey);
        foundCommercePayment.fail(reason);
        saveCommercePayment(foundCommercePayment);
    }

    /**
     * 결제 성공 처리
     */
    @Transactional
    public void saveSuccessPayment(String transactionKey) {
        CommercePayment foundCommercePayment = findByTransactionKey(transactionKey);
        foundCommercePayment.success();
        saveCommercePayment(foundCommercePayment);
    }
}
