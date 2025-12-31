package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.OrderRepository;
import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import jakarta.persistence.OptimisticLockException;
import com.loopers.domain.order.event.OrderCreatedEvent;
import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderFacade {
    private final OrderRepository orderRepository;
    private final PointRepository pointRepository;
    private final ProductRepository productRepository;
    private final ApplicationEventPublisher eventPublisher;


    @Transactional
    public Order createOrder(String userId, List<OrderItem> orderItems){

        BigDecimal totalAmount = orderItems.stream()
                .map(OrderItem::calculateTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        for(OrderItem orderItem : orderItems){
            int attempt = 0;
            final int maxRetries = 10; // reduced retry attempts
            while (true) {
                try {
                    //1. 상품 조회
                    Product product = productRepository.findById(orderItem.getProductId())
                            .orElseThrow(() -> new CoreException(ErrorType.BAD_REQUEST));

                    //2. 재고 부족 시 예외 처리
                    if(product.getStockQuantity() < orderItem.getQuantity()){
                        throw new CoreException(ErrorType.BAD_REQUEST);
                    }

                    //3. 재고 차감
                    product.decreaseStock(orderItem.getQuantity());
                    productRepository.save(product);
                    break;

                    // 낙관적 락 처리
                } catch (ObjectOptimisticLockingFailureException | OptimisticLockException e) {
                    attempt++;
                    if (attempt >= maxRetries) {
                        throw e;
                    }
                    try {
                        long backoffMs = Math.min(100L, 10L * attempt); // incremental backoff up to 100ms
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new CoreException(ErrorType.INTERNAL_ERROR);
                    }
                }
            }
        }

        //4. 포인트 차감
        Point point = pointRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new CoreException(ErrorType.BAD_REQUEST));

        if (point.getPointAmount().compareTo(totalAmount) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST);
        }

        point.usePoints(totalAmount);
        pointRepository.save(point);


        // 5. 주문 생성
        Order order = Order.createOrder(userId, orderItems);
        orderRepository.save(order);

        // 6. 주문 생성 이벤트 발행
        OrderCreatedEvent event = OrderCreatedEvent.from(order);
        eventPublisher.publishEvent(event);
        log.info("주문 생성 이벤트 발행: {}", order.getId());

        return order;
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByUserId(String userId){
        return orderRepository.findAllByUserId(userId);
    }
}
