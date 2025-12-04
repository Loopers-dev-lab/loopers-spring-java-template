package com.loopers.domain.payment;

import java.util.Optional;

public interface CommercePaymentRepository {
    Optional<CommercePayment> save(CommercePayment commercePayment);
    Optional<CommercePayment> findByTransactionKey(String transactionKey);
}

