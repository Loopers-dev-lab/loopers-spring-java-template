package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.PaymentCallBackRepository;
import com.loopers.domain.payment.PaymentCallbackModel;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class PaymentCallBackRepositoryImpl implements PaymentCallBackRepository {
    private final PaymentCallBackJpaRepository paymentCBJpaRepository;

    public PaymentCallBackRepositoryImpl(PaymentCallBackJpaRepository paymentCBJpaRepository) {
        this.paymentCBJpaRepository = paymentCBJpaRepository;
    }

    @Override
    public PaymentCallbackModel save(PaymentCallbackModel paymentCallbackModel) {
        return paymentCBJpaRepository.save(paymentCallbackModel);
    }

    @Override
    public Optional<PaymentCallbackModel> findById(Long id) {
        return paymentCBJpaRepository.findById(id);
    }

    @Override
    public void deleteById(Long id) {
        paymentCBJpaRepository.deleteById(id);
    }
}
