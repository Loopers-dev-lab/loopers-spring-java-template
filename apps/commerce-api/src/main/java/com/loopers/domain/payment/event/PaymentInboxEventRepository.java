package com.loopers.domain.payment.event;

import com.loopers.domain.event.BaseInboxEventRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentInboxEventRepository extends BaseInboxEventRepository<PaymentInboxEvent> {
}

