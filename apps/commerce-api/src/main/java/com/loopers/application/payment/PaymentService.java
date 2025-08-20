package com.loopers.application.payment;

import com.loopers.domain.payment.PaymentRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;

    public PaymentService(@Qualifier("paymentRepositoryImpl") PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }
    // PaymentService는 결제 관련 비즈니스 로직을 처리하는 서비스입니다.
    // 현재는 빈 상태로, 필요한 메서드를 추가하여 결제 처리 로직을 구현할 수 있습니다.

    // 예시 메서드: 결제 요청 처리
    public void processPayment() {
        // 결제 처리 로직을 여기에 구현
    }
}
