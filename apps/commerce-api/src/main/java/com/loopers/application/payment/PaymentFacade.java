package com.loopers.application.payment;

import com.loopers.application.payment.event.PaymentCompletedEvent;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderRepository;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentRepository;
import com.loopers.infrastructure.payment.client.PgClient;
import com.loopers.infrastructure.payment.client.PgClientService;
import com.loopers.infrastructure.payment.client.dto.PgClientDto;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Payment Facade (Application Layer)
 * - 트랜잭션 경계 관리
 * - PG 시스템과 도메인 객체 조합
 * - 부가 로직은 이벤트로 분리 (주문 완료 처리)
 */
@Service
@RequiredArgsConstructor
public class PaymentFacade {

    private static final Logger log = LoggerFactory.getLogger(PaymentFacade.class);

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PgClientService pgClientService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 결제 요청
     * - Payment 도메인 생성
     * - PG에 결제 요청
     * - PG 트랜잭션 키 할당
     *
     * @param command 결제 요청 Command
     * @return 결제 정보
     */
    @Transactional
    public PaymentInfo requestPayment(PaymentCommand.RequestPayment command) {
        command.validate();

        // 1. 주문 존재 여부 및 권한 확인
        Order order = orderRepository.findById(command.getOrderId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 주문입니다: " + command.getOrderId()));

        if (!order.isOwnedBy(command.getUserId())) {
            throw new CoreException(ErrorType.BAD_REQUEST, "본인의 주문만 결제할 수 있습니다");
        }

        if (!order.isPending()) {
            throw new CoreException(ErrorType.CONFLICT, "대기 중인 주문만 결제할 수 있습니다");
        }

        // 2. Payment 도메인 생성
        Payment payment = Payment.create(
                command.getUserId(),
                command.getOrderId(),
                command.getCardType(),
                command.getCardNo(),
                command.getAmount()
        );

        // 3. Payment 저장 (PENDING 상태)
        payment = paymentRepository.save(payment);

        // 4. PG에 결제 요청
        PgClientDto.PaymentRequest pgRequest = new PgClientDto.PaymentRequest(
                String.valueOf(command.getOrderId()),
                toPgCardType(command.getCardType()),
                command.getCardNo(),
                command.getAmount().longValue(),
                command.getCallbackUrl()
        );

        try {
            PgClient.ApiResponse<PgClientDto.TransactionResponse> pgResponse =
                    pgClientService.requestPayment(command.getUserId(), pgRequest);

            // 5. PG 트랜잭션 키 할당 (PG 응답이 성공한 경우)
            if (pgResponse.success() && pgResponse.data().transactionKey() != null) {
                payment.assignPgTransactionKey(pgResponse.data().transactionKey());
                payment = paymentRepository.save(payment);

                log.info("PG 결제 요청 성공: userId={}, orderId={}, pgTransactionKey={}",
                        command.getUserId(), command.getOrderId(), pgResponse.data().transactionKey());
            } else {
                log.warn("PG 결제 요청 실패 (Fallback): userId={}, orderId={}",
                        command.getUserId(), command.getOrderId());
            }
        } catch (Exception e) {
            // PG 요청 실패 시에도 Payment는 PENDING 상태로 유지 (나중에 재확인 가능)
            log.error("PG 결제 요청 중 예외 발생: userId={}, orderId={}, error={}",
                    command.getUserId(), command.getOrderId(), e.getMessage());
        }

        return PaymentInfo.from(payment);
    }

    /**
     * 콜백 처리 (PG로부터 결제 결과 수신)
     * - Payment 상태 업데이트
     * - 주문 완료 처리는 이벤트로 분리
     *
     * @param command 콜백 Command
     */
    @Transactional
    public void handleCallback(PaymentCommand.UpdatePaymentStatus command) {
        log.info("PG 콜백 수신: pgTransactionKey={}, status={}", command.getPgTransactionKey(), command.getStatus());

        // 1. Payment 조회
        Payment payment = paymentRepository.findByPgTransactionKey(command.getPgTransactionKey())
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND, "결제 정보를 찾을 수 없습니다: " + command.getPgTransactionKey()));

        // 2. Payment 상태 업데이트
        if ("SUCCESS".equals(command.getStatus())) {
            payment.success();
            log.info("결제 성공: pgTransactionKey={}, orderId={}", command.getPgTransactionKey(), payment.getOrderId());
        } else if ("FAILED".equals(command.getStatus())) {
            payment.fail(command.getReason());
            log.warn("결제 실패: pgTransactionKey={}, orderId={}, reason={}",
                    command.getPgTransactionKey(), payment.getOrderId(), command.getReason());
        }

        paymentRepository.save(payment);

        // 3. 결제 완료 이벤트 발행 (주문 완료 처리, 데이터 플랫폼 전송 등)
        eventPublisher.publishEvent(PaymentCompletedEvent.from(payment));
    }

    /**
     * 결제 상태 확인 및 동기화 (스케줄러용)
     * - PG에 상태 조회
     * - Payment 상태 동기화
     * - 주문 완료 처리는 이벤트로 분리
     *
     * @param paymentId Payment ID
     */
    @Transactional
    public void syncPaymentStatus(Long paymentId) {
        // 1. Payment 조회
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "결제 정보를 찾을 수 없습니다: " + paymentId));

        // PG 트랜잭션 키가 없으면 스킵 (PG 요청 실패한 경우)
        if (payment.getPgTransactionKey() == null) {
            log.warn("PG 트랜잭션 키가 없어 상태 동기화를 스킵합니다: paymentId={}", paymentId);
            return;
        }

        // 이미 최종 상태면 스킵
        if (payment.isSuccess() || payment.isFailed()) {
            return;
        }

        try {
            // 2. PG에 상태 조회
            PgClient.ApiResponse<PgClientDto.TransactionDetailResponse> pgResponse =
                    pgClientService.getTransaction(payment.getUserId(), payment.getPgTransactionKey());

            if (!pgResponse.success() || pgResponse.data() == null) {
                log.warn("PG 상태 조회 실패: pgTransactionKey={}", payment.getPgTransactionKey());
                return;
            }

            // 3. Payment 상태 동기화
            PgClientDto.TransactionStatus pgStatus = pgResponse.data().status();
            if (pgStatus == PgClientDto.TransactionStatus.SUCCESS) {
                payment.success();
                log.info("결제 상태 동기화 완료 (SUCCESS): paymentId={}, orderId={}", paymentId, payment.getOrderId());
            } else if (pgStatus == PgClientDto.TransactionStatus.FAILED) {
                payment.fail(pgResponse.data().reason());
                log.info("결제 상태 동기화 완료 (FAILED): paymentId={}, reason={}", paymentId, pgResponse.data().reason());
            }

            paymentRepository.save(payment);

            // 4. 결제 완료 이벤트 발행 (주문 완료 처리, 데이터 플랫폼 전송 등)
            eventPublisher.publishEvent(PaymentCompletedEvent.from(payment));
        } catch (Exception e) {
            log.error("결제 상태 동기화 중 예외 발생: paymentId={}, error={}", paymentId, e.getMessage());
        }
    }

    /**
     * 결제 상세 조회
     *
     * @param paymentId Payment ID
     * @param userId    사용자 ID (권한 확인용)
     * @return 결제 정보
     */
    @Transactional(readOnly = true)
    public PaymentInfo getPaymentDetail(Long paymentId, String userId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "결제 정보를 찾을 수 없습니다: " + paymentId));

        if (!payment.isOwnedBy(userId)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "본인의 결제 정보만 조회할 수 있습니다");
        }

        return PaymentInfo.from(payment);
    }

    // === Helper Methods ===

    /**
     * CardType을 PG CardType으로 변환
     */
    private PgClientDto.CardType toPgCardType(com.loopers.domain.payment.CardType cardType) {
        return switch (cardType) {
            case SAMSUNG -> PgClientDto.CardType.SAMSUNG;
            case KB -> PgClientDto.CardType.KB;
            case HYUNDAI -> PgClientDto.CardType.HYUNDAI;
        };
    }
}
