package com.loopers.infrastructure.payment.event;

import com.loopers.infrastructure.event.BaseInboxEventRepository;
import com.loopers.domain.payment.event.PaymentInboxEvent;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentInboxEventRepository extends BaseInboxEventRepository<PaymentInboxEvent> {
}

