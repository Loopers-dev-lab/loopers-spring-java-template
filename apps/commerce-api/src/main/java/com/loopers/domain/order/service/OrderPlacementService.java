package com.loopers.domain.order.service;

import com.loopers.domain.common.vo.Money;
import com.loopers.domain.members.repository.MemberRepository;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.command.OrderLineCommand;
import com.loopers.domain.order.command.OrderPlacementCommand;
import com.loopers.domain.order.repository.OrderRepository;
import com.loopers.domain.points.Point;
import com.loopers.domain.points.repository.PointRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.repository.ProductRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Component
public class OrderPlacementService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final PointRepository pointRepository;

    @Transactional
    public Order placeOrder(OrderPlacementCommand command) {
        validateMemberExists(command.getMemberId());

        List<OrderItem> items = processOrderLines(command.getOrderLines());
        Money totalPrice = items.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(Money.zero(), Money::plus);

        payWithPoints(command.getMemberId(), totalPrice);

        Order order = Order.create(command.getMemberId(), items);
        return orderRepository.save(order);
    }

    private List<OrderItem> processOrderLines(List<OrderLineCommand> orderLines) {
        List<OrderItem> items = new ArrayList<>();
        
        for (OrderLineCommand line : orderLines) {
            Product product = productRepository.findById(line.getProductId())
                    .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));

            int updatedRows = productRepository.decreaseStock(product.getId(), line.getQuantity());
            if (updatedRows == 0) {
                throw new CoreException(ErrorType.BAD_REQUEST, "재고가 부족합니다.");
            }

            items.add(new OrderItem(product.getId(), line.getQuantity(), product.getPrice()));
        }
        
        return items;
    }

    private void validateMemberExists(String memberId) {
        if (!memberRepository.existsByMemberId(memberId)) {
            throw new CoreException(ErrorType.NOT_FOUND, "회원을 찾을 수 없습니다.");
        }
    }

    private void payWithPoints(String memberId, Money totalPrice) {
        Point points = pointRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "포인트 정보를 찾을 수 없습니다."));

        if (!points.canAfford(totalPrice.getAmount())) {
            throw new CoreException(ErrorType.BAD_REQUEST, "포인트가 부족합니다.");
        }

        points.pay(totalPrice.getAmount());
        pointRepository.save(points);
    }
}
