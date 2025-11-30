package com.loopers.core.service.payment;

import com.loopers.core.domain.order.Order;
import com.loopers.core.domain.order.OrderItem;
import com.loopers.core.domain.order.repository.OrderItemRepository;
import com.loopers.core.domain.order.repository.OrderRepository;
import com.loopers.core.domain.order.vo.CouponId;
import com.loopers.core.domain.order.vo.OrderId;
import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.repository.PaymentRepository;
import com.loopers.core.domain.payment.vo.CardNo;
import com.loopers.core.domain.payment.vo.CardType;
import com.loopers.core.domain.payment.vo.PayAmount;
import com.loopers.core.domain.user.User;
import com.loopers.core.domain.user.UserPoint;
import com.loopers.core.domain.user.repository.UserPointRepository;
import com.loopers.core.domain.user.repository.UserRepository;
import com.loopers.core.domain.user.vo.UserIdentifier;
import com.loopers.core.service.payment.command.PaymentCommand;
import com.loopers.core.service.payment.component.OrderLineAggregator;
import com.loopers.core.service.payment.component.PayAmountDiscountStrategy;
import com.loopers.core.service.payment.component.PayAmountDiscountStrategySelector;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final UserRepository userRepository;
    private final UserPointRepository userPointRepository;
    private final PaymentRepository paymentRepository;
    private final PayAmountDiscountStrategySelector discountStrategySelector;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderLineAggregator orderLineAggregator;

    @Transactional
    public Payment pay(PaymentCommand command) {
        User user = userRepository.getByIdentifier(new UserIdentifier(command.userIdentifier()));
        Order order = orderRepository.getById(new OrderId(command.orderId()));
        List<OrderItem> orderItems = orderItemRepository.findAllByOrderId(order.getId());
        CouponId couponId = new CouponId(command.couponId());

        UserPoint userPoint = userPointRepository.getByUserIdWithLock(user.getId());
        PayAmountDiscountStrategy discountStrategy = discountStrategySelector.select(couponId);
        PayAmount payAmount = orderLineAggregator.aggregate(orderItems);
        PayAmount discountedPayAmount = discountStrategy.discount(payAmount, couponId);
        userPointRepository.save(userPoint.pay(discountedPayAmount));
        return paymentRepository.save(
                Payment.create(order.getId(), user.getId(), new CardType(command.cardType()), new CardNo(command.cardNo()), discountedPayAmount)
        );
    }
}
