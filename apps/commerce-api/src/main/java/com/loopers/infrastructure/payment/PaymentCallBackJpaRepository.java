package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.PaymentCallbackModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentCallBackJpaRepository extends JpaRepository<PaymentCallbackModel, Long> {
    // JpaRepository를 상속받아 PaymentCallbackModel에 대한 CRUD 작업을 수행할 수 있습니다.
    // 추가적인 메서드가 필요하다면 여기에 정의할 수 있습니다.
}
