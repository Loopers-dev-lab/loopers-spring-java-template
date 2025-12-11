package com.loopers.domain.payment.event;

import com.loopers.domain.coupon.event.CouponEvents;
import com.loopers.domain.payment.*;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventListener {

    private final PgPaymentGateway pgPaymentGateway;
    private final PaymentService paymentService;
    private final PaymentEventPublisher paymentEventPublisher;

    @Value("${pg.api.callbackUrl}")
    private String pgCallbackUrl;

    /**
     * PG 콜백 처리 핸들러
     * PaymentFacade에서 PG 콜백을 받아 발행한 이벤트를 처리
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentCallbackReceived(PaymentEvents.CallbackReceived event) {
        log.info("PaymentEventListener: PaymentCallbackReceivedEvent 수신 - orderId: {}, transactionKey: {}, status: {}", 
                event.orderId(), event.transactionKey(), event.status());

        // 결제 상태에 따라 처리
        if (event.status() == PaymentDto.PaymentStatus.FAILED) {
            // 결제 실패 처리
            paymentService.saveFailedPayment(event.transactionKey(), event.reason());
            log.info("결제 실패 처리 완료 - orderId: {}, transactionKey: {}, reason: {}", 
                    event.orderId(), event.transactionKey(), event.reason());
            
            // 주문 보상 이벤트 발행
            paymentEventPublisher.publishPaymentProcessingFailed(
                new PaymentEvents.ProcessingFailed(
                    event.orderId(), 
                    null,  // PG 콜백 경로에서는 originalEvent 없음
                    event.reason()
                )
            );
        } else {
            // 결제 성공 처리
            paymentService.saveSuccessPayment(event.transactionKey());
            log.info("결제 성공 처리 완료 - orderId: {}, transactionKey: {}", 
                    event.orderId(), event.transactionKey());
            
            // 결제 성공 이벤트 발행
            // PG 콜백 경로에서는 userId와 finalAmount를 알 수 없으므로,
            // CommercePayment에서 조회하거나 별도 처리 필요
            // 일단 기본값으로 처리 (실제로는 CommercePayment 조회 필요)
            paymentEventPublisher.publishPaymentProcessed(
                new PaymentEvents.Processed(
                    event.orderId(), 
                    null,  // userId는 CommercePayment에서 조회 필요
                    null,  // finalAmount는 CommercePayment에서 조회 필요
                    null   // PG 콜백 경로이므로 originalEvent는 null
                )
            );
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleCouponProcessed(CouponEvents.Processed event) {
        log.info("PaymentEventListener: CouponProcessedEvent 수신 - orderId: {}", event.orderId());

        // 이벤트에서 필요한 데이터 가져오기
        Long userId = event.userId();
        BigDecimal totalPrice = event.originalEvent().originalEvent().totalPrice();
        BigDecimal totalDiscountAmount = event.totalDiscountAmount();
        
        // 최종 결제 금액 계산 (할인 금액 제외)
        BigDecimal finalAmount = totalPrice.subtract(totalDiscountAmount);
        
        ApiResponse<PaymentDto.PgResponse> pgApiResponse = null;

        if (finalAmount.compareTo(BigDecimal.ZERO) > 0) {
            pgApiResponse = pgPaymentGateway.approvePayment(
                    userId,
                    PaymentDto.PgRequest.builder()
                            .orderId(String.format("%06d", event.orderId()))  // orderId를 문자열로 변환
                            .cardNo("1111-2222-3333-4444")
                            .cardType(PaymentDto.CardType.SAMSUNG)
                            .amount(finalAmount.longValue())
                            .callbackUrl(pgCallbackUrl)
                            .build()
            );
        }

        PaymentDto.PgResponse pgResponse = (pgApiResponse != null) ? pgApiResponse.data() : null;

        if (pgResponse != null && pgResponse.status() == PaymentDto.PaymentStatus.PENDING) {
            paymentService.saveCommercePayment(CommercePayment.builder()
                    .orderId(event.orderId())
                    .transactionKey(pgResponse.transactionKey())
                    .method(PaymentDto.PaymentMethod.CARD)
                    .cardType(PaymentDto.CardType.SAMSUNG)
                    .cardNo("1111-2222-3333-4444")
                    .paymentStatus(PaymentDto.PaymentStatus.PENDING)
                    .amount(finalAmount)
                    .build()
            );
            log.info("PG API 호출 및 결제 정보 저장 성공 - orderId: {}", event.orderId());
            paymentEventPublisher.publishPaymentProcessed(new PaymentEvents.Processed(
                event.orderId(), 
                userId,
                finalAmount,
                event
            ));
        } else {
            String failureReason = (pgResponse != null && pgResponse.reason() != null) ? pgResponse.reason() : "결제 요청에 실패했습니다.";
            log.error("PG API 호출 실패 - orderId: {}, reason: {}", event.orderId(), failureReason);
            paymentEventPublisher.publishPaymentProcessingFailed(new PaymentEvents.ProcessingFailed(
                event.orderId(), 
                event,  // 재고 원복을 위해 포함
                failureReason
            ));
        }
        
    }
}
