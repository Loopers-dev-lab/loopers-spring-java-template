package com.loopers.infrastructure.payment.event;

import com.loopers.infrastructure.event.BaseOutboxEventRepository;
import com.loopers.domain.payment.event.PaymentOutboxEvent;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentOutboxEventRepository extends BaseOutboxEventRepository<PaymentOutboxEvent> {
}

