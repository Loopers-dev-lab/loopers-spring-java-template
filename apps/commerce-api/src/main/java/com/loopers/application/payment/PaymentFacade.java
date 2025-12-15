package com.loopers.application.payment;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.*;
import com.loopers.domain.payment.event.PaymentFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentFacade {

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final PaymentStrategyFactory paymentStrategyFactory;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${payment.callback.base-url}")
    private String callbackBaseUrl;

    @Value("${payment.callback.path}")
    private String callbackPath;

    /**
     * 포인트 결제 처리
     */
    @Transactional
    public PaymentInfo payWithPoint(PaymentPointCommand command) {
        log.info("포인트 결제 시작: orderId={}, loginId={}, idempotencyKey={}",
                command.orderId(), command.userId(), command.idempotencyKey());

        // 중복 요청 검증
        Optional<Payment> existingPayment = paymentService.getPaymentByIdempotencyKey(command.idempotencyKey());
        if (existingPayment.isPresent()) {
            log.warn("중복 결제 요청 감지: orderId={}, idempotencyKey={}",
                    command.orderId(), command.idempotencyKey());
            return PaymentInfo.from(existingPayment.get());
        }

        // 주문 조회 및 검증
        Order order = orderService.getOrderById(command.orderId());
        order.validatePayable();

        // 최종 금액 계산
        Long finalAmount = order.getTotalAmountValue() - command.discountAmount();

        // Payment 생성
        Payment payment = paymentService.createPointPayment(
                order.getId(),
                order.getUser().getId(),
                finalAmount,
                command.idempotencyKey()
        );

        // 주문 상태: 결제 진행 중
        order.markAsPaymentPending();
        orderService.save(order);

        // 결제 전략 실행
        PaymentStrategy strategy = paymentStrategyFactory.getStrategy(PaymentMethod.POINT);
        PaymentInfo result = strategy.pay(payment);

        // 결제 성공 시 주문 완료
        if (payment.isSuccess()) {
            order.markAsCompleted();
            orderService.save(order);
        }

        return result;
    }

    /**
     * PG 카드 결제 요청
     */
    @Transactional
    public PaymentInfo payWithPgCard(PaymentPgCardCommand command) {
        log.info("PG 카드 결제 시작: orderId={}, loginId={}, cardType={}, idempotencyKey={}",
                command.orderId(), command.userId(), command.cardType(), command.idempotencyKey());

        // 중복 요청 검증
        Optional<Payment> existingPayment = paymentService.getPaymentByIdempotencyKey(command.idempotencyKey());
        if (existingPayment.isPresent()) {
            log.warn("중복 결제 요청 감지: orderId={}, idempotencyKey={}",
                    command.orderId(), command.idempotencyKey());
            return PaymentInfo.from(existingPayment.get());
        }

        // 주문 조회 및 검증
        Order order = orderService.getOrderById(command.orderId());
        order.validatePayable();

        // 최종 금액 계산
        Long finalAmount = order.getTotalAmountValue() - command.discountAmount();

        // Payment 생성
        String callbackUrl = callbackBaseUrl + callbackPath;
        Payment payment = paymentService.createPgCardPayment(
                order.getId(),
                order.getUser().getId(),
                command.cardType(),
                command.cardNo(),
                finalAmount,
                callbackUrl,
                command.idempotencyKey()
        );

        // 주문 상태: 결제 대기
        order.markAsPaymentPending();
        orderService.save(order);

        try {
            // 결제 전략 실행
            PaymentStrategy strategy = paymentStrategyFactory.getStrategy(PaymentMethod.PG_CARD);
            return strategy.pay(payment);

        } catch (Exception e) {
            log.error("PG 결제 요청 실패: orderId={}", order.getId(), e);

            // 주문 실패 처리
            order.markAsPaymentFailed();
            orderService.save(order);

            // 결제 실패 이벤트 발행
            eventPublisher.publishEvent(new PaymentFailedEvent(
                    order.getId(),
                    order.getUser().getId(),
                    order.getCouponId(),
                    e.getMessage()
            ));

            throw e;
        }
    }

    @Transactional(readOnly = true)
    public PaymentInfo getPaymentInfo(Long paymentId) {
        Payment payment = paymentService.getPayment(paymentId);
        return PaymentInfo.from(payment);
    }

    @Transactional(readOnly = true)
    public PaymentInfo getPaymentInfoByOrderId(Long orderId) {
        Payment payment = paymentService.getPaymentByOrderId(orderId);
        return PaymentInfo.from(payment);
    }
}
