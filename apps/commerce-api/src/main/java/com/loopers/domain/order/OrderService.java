package com.loopers.domain.order;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;

    /**
     * 주문 생성
     */
    @Transactional
    public Order saveOrder(Order order) {
        return orderRepository.save(order)
                .orElseThrow(() -> new CoreException(
                        ErrorType.INTERNAL_ERROR,
                        "Order 저장에 실패했습니다."
                ));
    }

    /**
     * 주문 조회
     */
    @Transactional(readOnly = true)
    public Order findOrderById(Long orderId) {
        log.info("Order 조회 시도 - orderId: {}", orderId);
        var orderOpt = orderRepository.findById(orderId);
        if (orderOpt.isEmpty()) {
            log.error("Order를 찾을 수 없습니다 - orderId: {}", orderId);
            throw new CoreException(
                    ErrorType.NOT_FOUND,
                    "[orderId = " + orderId + "] Order를 찾을 수 없습니다."
            );
        }
        Order order = orderOpt.get();
        log.info("Order 조회 성공 - orderId: {}, orderStatus: {}", orderId, order.getOrderStatus());
        return order;
    }

    /**
     * 유저의 주문 목록 조회
     */
    @Transactional(readOnly = true)
    public List<Order> findOrdersByUserId(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    /**
     * 실패한 주문 저장
     * REQUIRES_NEW로 별도 트랜잭션에서 실행되므로, 엔티티를 다시 조회해서 사용
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Order saveFailedOrder(Long orderId, String errorMessage) {
        // 새로운 트랜잭션에서 엔티티를 다시 조회하여 영속성 컨텍스트 충돌 방지
        Order foundOrder = findOrderById(orderId);
        foundOrder.fail(errorMessage);
        return saveOrder(foundOrder);
    }

    /**
     * 주문 성공 처리
     */
    @Transactional
    public Order saveSuccessOrder(Long orderId) {
        // 새로운 트랜잭션에서 엔티티를 다시 조회하여 영속성 컨텍스트 충돌 방지
        Order foundOrder = findOrderById(orderId);
        foundOrder.confirm();
        return saveOrder(foundOrder);
    }

}

