package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.CommercePayment;
import com.loopers.domain.payment.CommercePaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class CommercePaymentRepositoryImpl implements CommercePaymentRepository {

    private final CommercePaymentJpaRepository commercePaymentJpaRepository;

    @Override
    public Optional<CommercePayment> save(CommercePayment commercePayment) {
        CommercePayment savedPayment = commercePaymentJpaRepository.save(commercePayment);
        return Optional.of(savedPayment);
    }

    @Override
    public Optional<CommercePayment> findByTransactionKey(String transactionKey) {
        return commercePaymentJpaRepository.findByTransactionKey(transactionKey);
    }
}

