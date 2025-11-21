package com.loopers.domain.order;

import com.loopers.application.order.OrderItemContext;
import com.loopers.domain.common.vo.Money;
import com.loopers.domain.point.Point;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderDomainService {

    public void processPaymentAndInventory(Order order, List<OrderItemContext> orderItemContexts, Point point) {
        int totalAmount = 0;

        for (OrderItemContext item : orderItemContexts) {

            // 주문 상품별 재고 차감
            item.stock().decrease(item.orderQuantity());

            // 전체 금액 계산
            totalAmount += item.orderPrice().getAmount().intValue();
        }

        // 결제 : 유저 포인트 차감
        point.use(Money.of(totalAmount));

        // 결제 완료 (PAID) : 주문 상태 변경
        order.changeToPaid();
    }
}
