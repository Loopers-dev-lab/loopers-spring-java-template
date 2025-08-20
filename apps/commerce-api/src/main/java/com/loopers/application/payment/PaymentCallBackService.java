package com.loopers.application.payment;

import com.loopers.domain.payment.PaymentCallBackRepository;
import org.springframework.stereotype.Service;

@Service
public class PaymentCallBackService {
    private final PaymentCallBackRepository repo;

    public PaymentCallBackService(PaymentCallBackRepository repo) {
        this.repo = repo;
    }
    // PaymentCallBackService는 결제 콜백 관련 비즈니스 로직을 처리하는 서비스입니다.
    // 현재는 빈 상태로, 필요한 메서드를 추가하여 결제 콜백 처리 로직을 구현할 수 있습니다.

    // 예시 메서드: 결제 콜백 처리
    public void processPaymentCallback() {
    }
}
