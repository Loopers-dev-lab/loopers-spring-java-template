package com.loopers.domain.order;

import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderDomainService {
    private final OrderRepository orderRepository;
    private final PointRepository pointRepository;
    private final ProductRepository productRepository;

    //주문 생성한다
    @Transactional
    public Order createOrder(String userId, List<OrderItem> orderItems){

        //총 주문 금액 계산
        BigDecimal totalAmount = orderItems.stream()
                .map(OrderItem::calculateTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        //재고 차감
        for(OrderItem orderItem : orderItems){
            Product product = productRepository.findById(orderItem.getProductId())
                    .orElseThrow(() -> new CoreException(ErrorType.BAD_REQUEST));

            if(product.getStockQuantity() < orderItem.getQuantity()){
                throw new CoreException(ErrorType.BAD_REQUEST);
            }

            product.decreaseStock(orderItem.getQuantity());
            productRepository.save(product);
        }

        //포인트 차감
        Point point = pointRepository.findByUserId(userId)
                .orElseThrow(() -> new CoreException(ErrorType.BAD_REQUEST));

        // 보유 포인트가 총액보다 적으면 예외
        if (point.getPointAmount().compareTo(totalAmount) < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST);
        }

        // 실제 포인트 차감 반영
        point.usePoints(totalAmount);
        pointRepository.save(point);

        // 주문 생성 (도메인 팩토리 사용 권장)
        return Order.createOrder(userId, orderItems);
    }

    //주문 내역 확인
    @Transactional(readOnly = true)
    public List<Order> getOrdersByUserId(String userId){
        return orderRepository.findAllByUserId(userId);
    }
}
