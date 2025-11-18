package com.loopers.domain.order;

import com.loopers.domain.order.repository.OrderRepository;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryOrderRepository implements OrderRepository {

    private final Map<Long, Order> store = new HashMap<>();
    private long sequence = 0L;

    @Override
    public Optional<Order> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Order save(Order order) {
        if (order.getId() == null) {
            Long newId = ++sequence;
            try {
                Field idField = order.getClass().getSuperclass().getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(order, newId);

                // OrderItem들에게 Order 할당 (bidirectional relationship)
                order.getItems().forEach(item -> item.assignOrder(order));
            } catch (Exception e) {
                throw new RuntimeException("Failed to set order id", e);
            }
            store.put(newId, order);
            return order;
        } else {
            store.put(order.getId(), order);
            return order;
        }
    }

    public void clear() {
        store.clear();
        sequence = 0L;
    }
}