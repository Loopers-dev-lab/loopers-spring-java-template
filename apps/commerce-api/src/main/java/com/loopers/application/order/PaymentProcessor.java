package com.loopers.application.order;

import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.payment.*;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 처리를 별도 트랜잭션으로 분리하기 위한 컴포넌트
 * - PG 장애 시 Payment는 PENDING 상태로 저장되고
 * - 메인 트랜잭션(주문, 재고)은 롤백되지 않음
 * - 스케줄러가 나중에 PENDING Payment를 재확인 가능
 */
@Component
@RequiredArgsConstructor
public class PaymentProcessor {

    @Value("${payment.callback.base-url}")
    private String callbackBaseUrl;

    private final PaymentService paymentService;
    private final PaymentGateway paymentGateway;
    private final com.loopers.infrastructure.order.OrderJpaRepository orderJpaRepository;

    /**
     * 포인트 결제 처리 (동기, 트랜잭션 분리 없음)
     * 1. Payment 생성 (PENDING)
     * 2. 포인트 차감
     * 3. Payment 즉시 완료 (PENDING → SUCCESS)
     * 4. Payment 저장
     * 5. 주문 완료 처리
     */
    public void processPointPayment(User user, Order order) {
        // 1. Payment 생성 (PENDING)
        Payment payment = Payment.createPaymentForPoint(
                order,
                order.getTotalPrice(),
                PaymentType.POINT
        );

        // 2. 포인트 차감
        user.usePoint(order.getTotalPrice());

        // 3. Payment 즉시 완료 (PENDING → SUCCESS)
        payment.completePointPayment();

        // 4. Payment 저장
        paymentService.save(payment);

        // 5. 주문 완료 처리
        order.completeOrder();
    }

    /**
     * 카드 결제 처리 (비동기 - PG 연동, 별도 트랜잭션)
     * 1. Order 재조회 (새 트랜잭션에서 영속 상태로 관리)
     * 2. Payment 생성
     * 3. PG 결제 요청
     * 4. Payment 상태를 PROCESSING으로 변경
     * 5. 주문 접수 상태로 변경
     * 6. 콜백으로 최종 결과 처리 (PaymentFacade)
     *
     * PG 장애 시 Payment만 저장되고 메인 트랜잭션(주문, 재고)은 롤백
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processCardPayment(OrderCommand command, Long orderId) {
        // 1. 새 트랜잭션에서 Order 재조회
        Order order = orderJpaRepository.findById(orderId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다"));

        // 2. Payment 생성
        Payment payment = Payment.createPaymentForCard(
                order,
                order.getTotalPrice(),
                command.paymentType(),
                command.cardType(),
                command.cardNo()
        );

        // 3. PG 결제 요청 (비동기)
        String callbackUrl = callbackBaseUrl + "/api/v1/payments/callback";
        PaymentResult result = paymentGateway.processPayment(command.userId(), payment, callbackUrl);

        // 4. PG 결제 결과 확인
        if ("FAIL".equals(result.status())) {
            // Fallback이 호출된 경우: 결제 대기 상태로 처리
            // Payment는 이미 PENDING 상태로 생성되었으므로 상태 변경 불필요
            paymentService.save(payment);

            // 주문은 INIT 상태 유지 (나중에 재시도 가능)
            throw new CoreException(ErrorType.PAYMENT_REQUEST_FAILED,
                    "결제 시스템 장애로 인해 주문이 대기 상태입니다. 잠시 후 다시 시도해주세요.");
        }

        // 5. Payment 상태 → PROCESSING
        payment.startProcessing(result.transactionId());

        // 6. Payment 저장
        paymentService.save(payment);

        // 7. 주문 상태 → RECEIVED (접수 완료)
        order.updateStatus(OrderStatus.RECEIVED);
    }
}
