package com.loopers.application.payment;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.order.OrderEntity;
import com.loopers.domain.order.OrderService;
import com.loopers.domain.payment.*;
import com.loopers.domain.user.UserEntity;
import com.loopers.domain.user.UserService;
import com.loopers.infrastructure.payment.client.dto.PgPaymentResponse;
import com.loopers.interfaces.api.payment.PaymentV1Dtos;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 결제 유스케이스 Facade (응용 계층)
 * <p>
 * DDD 원칙에 따라 유스케이스 조정 역할만 담당:
 * - 트랜잭션 경계 설정
 * - 여러 도메인 서비스 조합
 * - 이벤트 발행
 * - DTO 변환
 * <p>
 * 비즈니스 로직은 도메인 계층(PaymentProcessor, PaymentValidator)에 위치
 *
 * @author hyunjikoh
 * @since 2025. 12. 3.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentFacade {

    // 도메인 서비스
    private final PaymentService paymentService;
    private final PaymentProcessor paymentProcessor;
    private final PaymentValidator paymentValidator;
    private final UserService userService;
    private final OrderService orderService;


    /**
     * 카드 결제 처리 유스케이스
     */
    @Transactional
    public PaymentInfo processPayment(PaymentCommand command) {
        // 1. 사용자 조회
        UserEntity user = userService.getUserByUsername(command.username());

        // 2. 주문 조회 및 검증
        OrderEntity order = orderService.getOrderByIdAndUserId(command.orderId(), user.getId());
        paymentValidator.validateOrderForPayment(order, command.amount());

        // 3. PG 결제 처리
        PaymentEntity payment = paymentProcessor.processPgPayment(user, command);

        // 4. DTO 변환 후 반환
        return PaymentInfo.from(payment);
    }

    /**
     * PG 콜백 처리 유스케이스
     * 멱등성 보장: PENDING 상태가 아니면 처리하지 않음
     */
    @Transactional
    public void handlePaymentCallback(PaymentV1Dtos.PgCallbackRequest request) {
        log.info("결제 콜백 처리 시작 - transactionKey: {}, status: {}, orderNumber: {}",
                request.transactionKey(), request.status(), request.orderId());

        // 1. DB에서 결제 조회
        PaymentEntity payment = paymentService.getByTransactionKey(request.transactionKey());

        // 2. 멱등성 체크: 이미 처리된 결제는 무시
        if (payment.getPaymentStatus() != PaymentStatus.PENDING) {
            log.warn("이미 처리된 결제 - transactionKey: {}, currentStatus: {}",
                    request.transactionKey(), payment.getPaymentStatus());
            return;
        }

        // 3. 사용자 및 주문 조회
        UserEntity user = userService.getUserById(payment.getUserId());
        OrderEntity order = orderService.getOrderByOrderNumberAndUserId(payment.getOrderNumber(), payment.getUserId());

        // 4. PG에서 실제 상태 조회
        PgPaymentResponse pgData = paymentProcessor.verifyPaymentFromPg(
                request.transactionKey(),
                user.getUsername()
        );

        // 5. 콜백 데이터 검증
        paymentValidator.validateCallbackData(payment, request, pgData);
        paymentValidator.validateOrderAmount(order, pgData.amount());

        // 6. 결제 결과에 따라 상태 업데이트 및 이벤트 발행
        paymentService.processPaymentResult(payment, request);
    }

}
