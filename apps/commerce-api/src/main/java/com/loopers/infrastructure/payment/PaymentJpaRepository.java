package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.PaymentModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentJpaRepository extends JpaRepository<PaymentModel, Long> {
    // PaymentJpaRepository는 결제 관련 데이터베이스 작업을 처리하는 JPA 레포지토리입니다.
    // 현재는 빈 상태로, 필요한 메서드를 추가하여 결제 관련 데이터베이스 작업을 구현할 수 있습니다.

    // 예시 메서드: 특정 사용자 ID로 결제 정보 조회
    // Optional<PaymentModel> findByUserId(Long userId);
}
