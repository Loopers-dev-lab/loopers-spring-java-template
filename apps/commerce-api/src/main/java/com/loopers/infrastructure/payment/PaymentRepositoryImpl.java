package com.loopers.infrastructure.payment;

import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentRepository;
import com.loopers.domain.payment.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Payment Repository 구현체
 */
@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepository {

    private final PaymentJpaRepository jpaRepository;

    @Override
    public Payment save(Payment payment) {
        // Domain -> Entity 변환
        PaymentEntity entity = PaymentEntity.from(payment);

        // 이미 존재하는 엔티티면 업데이트
        if (payment.getId() != null) {
            PaymentEntity existingEntity = jpaRepository.findById(payment.getId())
                    .orElseThrow(() -> new IllegalStateException("결제 정보를 찾을 수 없습니다: " + payment.getId()));
            existingEntity.updateFromDomain(payment);
            entity = existingEntity;
        }

        // 저장 후 Domain 반환
        PaymentEntity saved = jpaRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<Payment> findById(Long id) {
        return jpaRepository.findById(id)
                .map(PaymentEntity::toDomain);
    }

    @Override
    public Optional<Payment> findByPgTransactionKey(String pgTransactionKey) {
        return jpaRepository.findByPgTransactionKey(pgTransactionKey)
                .map(PaymentEntity::toDomain);
    }

    @Override
    public List<Payment> findByOrderId(Long orderId) {
        return jpaRepository.findByOrderId(orderId).stream()
                .map(PaymentEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Payment> findByUserIdAndOrderId(String userId, Long orderId) {
        return jpaRepository.findByUserIdAndOrderId(userId, orderId).stream()
                .map(PaymentEntity::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Payment> findPendingPayments() {
        return jpaRepository.findByStatus(PaymentStatus.PENDING).stream()
                .map(PaymentEntity::toDomain)
                .collect(Collectors.toList());
    }
}
