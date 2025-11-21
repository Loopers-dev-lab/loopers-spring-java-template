package com.loopers.domain.order;

import com.loopers.domain.user.User;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    /**
     * 주문 생성
     */
    @Transactional
    public Optional<Order> saveOrder(Order order) {
        return orderRepository.save(order);
    }

    /**
     * 주문 조회
     */
    @Transactional(readOnly = true)
    public Order findOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND,
                        "[orderId = " + orderId + "] Order를 찾을 수 없습니다."
                ));
    }

    /**
     * 유저의 주문 목록 조회
     */
    @Transactional(readOnly = true)
    public List<Order> findOrdersByUser(User user) {
        return orderRepository.findByUser(user);
    }
}

