package com.loopers.domain.payment;

import java.util.Optional;

public interface PaymentRepository {
    // PaymentRepository 인터페이스는 결제 관련 데이터베이스 작업을 정의합니다.
    // 필요한 메서드를 여기에 추가할 수 있습니다.
    // 예: 결제 생성, 조회, 업데이트, 삭제 등
    PaymentModel save(PaymentModel paymentModel);
    Optional<PaymentModel> findById(Long id);
    void deleteById(Long id);

}
