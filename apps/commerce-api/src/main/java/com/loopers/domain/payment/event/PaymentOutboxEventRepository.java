package com.loopers.domain.payment.event;

import com.loopers.domain.event.BaseOutboxEventRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentOutboxEventRepository extends BaseOutboxEventRepository<PaymentOutboxEvent> {
}

