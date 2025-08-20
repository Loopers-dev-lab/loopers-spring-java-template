package com.loopers.domain.payment;


import java.util.Optional;

public interface PaymentCallBackRepository {
    PaymentCallbackModel save(PaymentCallbackModel paymentCallbackModel);
    Optional<PaymentCallbackModel> findById(Long id);
    void deleteById(Long id);

}
