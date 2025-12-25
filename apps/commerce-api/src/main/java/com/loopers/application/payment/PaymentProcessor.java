package com.loopers.application.payment;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.*;
import com.loopers.domain.payment.event.CardPaymentProcessingStartedEvent;
import com.loopers.domain.payment.event.PaymentCompletedEvent;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 처리 Command 실행 컴포넌트
 *
 * Command 패턴으로 필수 결제 로직을 동기적으로 처리합니다.
 * 완료 후 진짜 Domain Event를 발행하여 다른 서비스에 알립니다.
 *
 * 포인트 결제: 동기 처리 후 즉시 완료 이벤트 발행
 * 카드 결제: Payment 저장 후 비동기 PG 처리 이벤트 발행
 *
 * - PG 장애 시 Payment는 PENDING 상태로 저장
 * - 스케줄러가 나중에 PENDING Payment를 재확인 가능
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentProcessor {

    private final PaymentService paymentService;
    private final UserService userService;
    private final OrderService orderService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 포인트 결제 처리 Command (새 트랜잭션에서 처리)
     *
     * @param userId 결제할 사용자 ID
     * @param orderId 결제할 주문 ID
     *
     * 처리 순서:
     * 1. User와 Order 조회 (영속 상태로 가져옴)
     * 2. 포인트 차감
     * 3. Payment 생성 및 완료 (PENDING → SUCCESS)
     * 4. Payment 저장
     * 5. 주문 완료 처리 (OrderStatus.COMPLETED)
     * 6. PaymentCompletedEvent 발행 (Domain Event)
     */
    @Transactional
    public void processPointPayment(Long userId, Long orderId) {
        log.info("[포인트 결제 시작] userId={}, orderId={}", userId, orderId);

        // 1. User와 Order 조회
        User user = userService.getUserById(userId);
        Order order = orderService.getOrderById(orderId);

        // 2. 포인트 차감
        user.usePoint(order.getTotalPrice());

        // 3. Payment 생성 및 완료 (PENDING → SUCCESS)
        Payment payment = Payment.createPointPayment(order, user);
        paymentService.save(payment);
        payment.completePointPayment();

        // 4. 주문 완료 처리
        order.completeOrder();

        // 5. 결제 완료 Domain Event 발행
        eventPublisher.publishEvent(
                PaymentCompletedEvent.of(
                        payment.getPaymentId(),
                        orderId,
                        userId,
                        PaymentType.POINT,
                        order.getTotalPrice().getAmount()
                )
        );

        log.info("[포인트 결제 완료] paymentId={}, orderId={}, amount={}",
                payment.getPaymentId(), orderId, order.getTotalPrice());
    }

    /**
     * 카드 결제 처리 Command
     *
     * @param orderId 결제할 주문 ID
     * @param cardType 카드 타입
     * @param cardNo 카드 번호
     *
     * 처리 순서:
     * 1. Order 조회
     * 2. Payment 생성 및 저장 (PENDING 상태)
     * 3. 주문 상태 업데이트 (PAYMENT_PENDING)
     * 4. CardPaymentProcessingStartedEvent 발행 (비동기 PG 호출을 위한 Event)
     *
     * CardPaymentProcessingStartedEvent를 수신한 PaymentEventListener가 비동기로 PG를 호출합니다.
     * PG 장애 시에도 Payment는 PENDING으로 저장되어 Scheduler가 재시도 가능합니다.
     */
    @Transactional
    public void processCardPayment(Long orderId, CardType cardType, String cardNo) {
        log.info("[카드 결제 시작] orderId={}, cardType={}", orderId, cardType);

        // 1. Order 조회
        Order order = orderService.getOrderById(orderId);

        // 2. Payment 생성 및 저장 (PENDING 상태)
        Payment payment = Payment.createCardPayment(order, cardType, cardNo);
        paymentService.save(payment);

        // 3. 주문 상태 업데이트 (PAYMENT_PENDING)
        order.startPaymentProcessing();

        // 4. PG 처리를 위한 Event 발행 (비동기 처리)
        eventPublisher.publishEvent(
                CardPaymentProcessingStartedEvent.of(
                        payment.getPaymentId(),
                        orderId,
                        String.valueOf(order.getUser().getId()),
                        cardType,
                        cardNo
                )
        );

        log.info("[카드 결제 Payment 생성 완료] paymentId={}, orderId={}, status=PENDING",
                payment.getPaymentId(), orderId);
    }
}
