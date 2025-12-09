package com.loopers.core.service.payment;

import com.loopers.core.domain.order.Order;
import com.loopers.core.domain.order.OrderItem;
import com.loopers.core.domain.order.repository.OrderItemRepository;
import com.loopers.core.domain.order.repository.OrderRepository;
import com.loopers.core.domain.order.vo.CouponId;
import com.loopers.core.domain.order.vo.OrderId;
import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.repository.PaymentRepository;
import com.loopers.core.domain.payment.type.PaymentStatus;
import com.loopers.core.domain.payment.vo.CardNo;
import com.loopers.core.domain.payment.vo.CardType;
import com.loopers.core.domain.payment.vo.PayAmount;
import com.loopers.core.domain.user.User;
import com.loopers.core.domain.user.repository.UserRepository;
import com.loopers.core.domain.user.vo.UserIdentifier;
import com.loopers.core.service.payment.command.PaymentCommand;
import com.loopers.core.service.payment.component.OrderLineAggregator;
import com.loopers.core.service.payment.component.PayAmountDiscountStrategy;
import com.loopers.core.service.payment.component.PayAmountDiscountStrategySelector;
import com.loopers.core.service.payment.event.PgPaymentEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final PayAmountDiscountStrategySelector discountStrategySelector;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderLineAggregator orderLineAggregator;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Payment pay(PaymentCommand command) {
        User user = userRepository.getByIdentifier(new UserIdentifier(command.userIdentifier()));
        Order order = orderRepository.getById(new OrderId(command.orderId()));
        List<OrderItem> orderItems = orderItemRepository.findAllByOrderId(order.getId());
        CouponId couponId = new CouponId(command.couponId());

        boolean hasSuccessfulPayment = paymentRepository.findByWithLock(order.getOrderKey(), PaymentStatus.SUCCESS).isPresent();
        if (hasSuccessfulPayment) {
            throw new IllegalStateException("이미 결제에 성공한 이력이 있는 주문입니다.");
        }

        boolean hasPendingPayment = paymentRepository.findByWithLock(order.getOrderKey(), PaymentStatus.PENDING).isPresent();
        if (hasPendingPayment) {
            throw new IllegalStateException("이미 결제가 진행 중인 주문입니다.");
        }
        // 재고 차감 및 주문 항목 저장
        PayAmount payAmount = orderLineAggregator.aggregate(orderItems);

        //결제 금액 계산
        PayAmountDiscountStrategy discountStrategy = discountStrategySelector.select(couponId);
        PayAmount discountedPayAmount = discountStrategy.discount(payAmount, couponId);
        Payment payment = Payment.create(order.getOrderKey(), user.getId(), new CardType(command.cardType()), new CardNo(command.cardNo()), discountedPayAmount);

        //결제 저장
        Payment savedPayment = paymentRepository.save(payment);
        eventPublisher.publishEvent(new PgPaymentEvent(savedPayment.getId(), couponId));

        return savedPayment;
    }
}
