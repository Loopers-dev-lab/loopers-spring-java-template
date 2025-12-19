package com.loopers.domain.payment;

import com.loopers.domain.order.Order;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryPaymentRepository implements PaymentRepository {

    private final Map<Long, Payment> store = new HashMap<>();
    private final Map<String, Payment> transactionKeyIndex = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    @Override
    public Payment save(Payment payment) {
        if (payment.getId() == null) {
            try {
                java.lang.reflect.Field idField = Payment.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(payment, idGenerator.getAndIncrement());
            } catch (Exception e) {
                throw new RuntimeException("Failed to set ID", e);
            }
        }
        store.put(payment.getId(), payment);
        if (payment.getTransactionKey() != null) {
            transactionKeyIndex.put(payment.getTransactionKey(), payment);
        }
        return payment;
    }

    @Override
    public Optional<Payment> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<Payment> findByOrder(Order order) {
        return store.values().stream()
                .filter(payment -> payment.getOrder().equals(order))
                .findFirst();
    }

    @Override
    public Optional<Payment> findByTransactionKey(String transactionKey) {
        return Optional.ofNullable(transactionKeyIndex.get(transactionKey));
    }

    @Override
    public boolean existsByOrder(Order order) {
        return findByOrder(order).isPresent();
    }

    @Override
    public List<Payment> findPendingPaymentsOlderThan(LocalDateTime dateTime) {
        return store.values().stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.PENDING)
                .filter(payment -> payment.getCreatedAt() != null &&
                                   payment.getCreatedAt().toLocalDateTime().isBefore(dateTime))
                .toList();
    }

    @Override
    public List<Payment> findByRequiresRetryTrue() {
        return store.values().stream()
                .filter(Payment::isRequiresRetry)
                .toList();
    }

    public void clear() {
        store.clear();
        transactionKeyIndex.clear();
    }
}
