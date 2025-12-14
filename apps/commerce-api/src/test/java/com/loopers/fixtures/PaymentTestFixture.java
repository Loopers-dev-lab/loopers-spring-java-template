package com.loopers.fixtures;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicLong;

import com.loopers.application.payment.PaymentCommand;
import com.loopers.domain.payment.PaymentDomainDtos;
import com.loopers.domain.payment.PaymentEntity;
import com.loopers.domain.payment.PaymentStatus;
import com.loopers.domain.user.UserEntity;

/**
 * Payment 관련 테스트 픽스처 클래스
 * 테스트에서 자주 사용되는 Payment 객체 생성 메서드들을 제공합니다.
 *
 * @author hyunjikoh
 * @since 2025. 12. 05.
 */
public class PaymentTestFixture {

    private static final AtomicLong ID_GENERATOR = new AtomicLong(1);

    // 기본 유효한 데이터
    public static final Long DEFAULT_ORDER_ID = 1L;
    public static final Long DEFAULT_ORDER_NUMBER = System.currentTimeMillis() * 1000000L + 123456L;

    public static final Long DEFAULT_USER_ID = 1L;
    public static final String DEFAULT_TRANSACTION_KEY = "TXN_" + System.currentTimeMillis();
    public static final BigDecimal DEFAULT_AMOUNT = new BigDecimal("50000.00");
    public static final String DEFAULT_CARD_TYPE = "CREDIT";
    public static final String DEFAULT_CARD_NO = "1234-5678-9012-3456";
    public static final String DEFAULT_CALLBACK_URL = "http://localhost:8080/api/v1/payments/callback";
    public static final PaymentStatus DEFAULT_STATUS = PaymentStatus.PENDING;

    // 실패 사유
    public static final String FAILURE_REASON_INSUFFICIENT_BALANCE = "잔액 부족";
    public static final String FAILURE_REASON_INVALID_CARD = "유효하지 않은 카드";
    public static final String FAILURE_REASON_TIMEOUT = "결제 타임아웃";

    /**
     * 기본값으로 PaymentDomainCreateRequest 생성
     */
    public static PaymentDomainDtos.PaymentDomainCreateRequest createDefaultDomainRequest() {
        return new PaymentDomainDtos.PaymentDomainCreateRequest(
                DEFAULT_USER_ID,
                DEFAULT_ORDER_ID,
                DEFAULT_TRANSACTION_KEY,
                DEFAULT_CARD_TYPE,
                DEFAULT_CARD_NO,
                DEFAULT_CALLBACK_URL,
                DEFAULT_AMOUNT,
                DEFAULT_STATUS,
                ZonedDateTime.now(),
                null
        );
    }

    /**
     * PENDING 상태의 PaymentDomainCreateRequest 생성
     */
    public static PaymentDomainDtos.PaymentDomainCreateRequest createPendingDomainRequest() {
        return new PaymentDomainDtos.PaymentDomainCreateRequest(
                DEFAULT_USER_ID,
                DEFAULT_ORDER_ID,
                null, // PENDING 상태에서는 transactionKey가 null
                DEFAULT_CARD_TYPE,
                DEFAULT_CARD_NO,
                DEFAULT_CALLBACK_URL,
                DEFAULT_AMOUNT,
                PaymentStatus.PENDING,
                ZonedDateTime.now(),
                null
        );
    }

    /**
     * 커스텀 PaymentDomainCreateRequest 생성
     */
    public static PaymentDomainDtos.PaymentDomainCreateRequest createDomainRequest(
            Long userId,
            Long orderId,
            String transactionKey,
            BigDecimal amount,
            PaymentStatus status
    ) {
        return new PaymentDomainDtos.PaymentDomainCreateRequest(
                userId,
                orderId,
                transactionKey,
                DEFAULT_CARD_TYPE,
                DEFAULT_CARD_NO,
                DEFAULT_CALLBACK_URL,
                amount,
                status,
                ZonedDateTime.now(),
                null
        );
    }

    /**
     * 커스텀 PaymentDomainCreateRequest 생성
     */
    public static PaymentDomainDtos.PaymentDomainCreateRequest createDomainRequestWithAmount(
            BigDecimal amount
    ) {
        return new PaymentDomainDtos.PaymentDomainCreateRequest(
                DEFAULT_USER_ID,
                DEFAULT_ORDER_ID,
                DEFAULT_TRANSACTION_KEY,
                DEFAULT_CARD_TYPE,
                DEFAULT_CARD_NO,
                DEFAULT_CALLBACK_URL,
                amount,
                DEFAULT_STATUS,
                ZonedDateTime.now(),
                null
        );
    }

    /**
     * 기본값으로 PaymentCommand 생성
     */
    public static PaymentCommand createDefaultCommand() {
        return new PaymentCommand(
                "testuser",
                DEFAULT_ORDER_ID,
                DEFAULT_ORDER_NUMBER,
                DEFAULT_CARD_TYPE,
                DEFAULT_CARD_NO,
                DEFAULT_AMOUNT,
                DEFAULT_CALLBACK_URL
        );
    }

    /**
     * 커스텀 금액으로 PaymentCommand 생성
     */
    public static PaymentCommand createCommandWithAmount(BigDecimal amount) {
        return new PaymentCommand(
                "testuser",
                DEFAULT_ORDER_ID,
                DEFAULT_ORDER_NUMBER,
                DEFAULT_CARD_TYPE,
                DEFAULT_CARD_NO,
                amount,
                DEFAULT_CALLBACK_URL
        );
    }

    /**
     * 커스텀 주문 ID로 PaymentCommand 생성
     */
    public static PaymentCommand createCommandWithOrderId(Long orderId) {
        return new PaymentCommand(
                "testuser",
                orderId,
                DEFAULT_ORDER_NUMBER,
                DEFAULT_CARD_TYPE,
                DEFAULT_CARD_NO,
                DEFAULT_AMOUNT,
                DEFAULT_CALLBACK_URL
        );
    }

    /**
     * 기본 PENDING 상태의 PaymentEntity 생성
     */
    public static PaymentEntity createPendingPayment() {
        return PaymentEntity.createPayment(createPendingDomainRequest());
    }

    /**
     * UserEntity와 PaymentCommand로 PENDING PaymentEntity 생성
     */
    public static PaymentEntity createPendingPayment(UserEntity user, PaymentCommand command) {
        return PaymentEntity.createPending(user, command);
    }

    /**
     * 완료된 상태의 PaymentEntity 생성
     */
    public static PaymentEntity createCompletedPayment() {
        PaymentEntity payment = createPendingPayment();
        payment.updateTransactionKey("TXN_COMPLETED_" + ID_GENERATOR.getAndIncrement());
        payment.complete();
        return payment;
    }

    /**
     * 실패한 상태의 PaymentEntity 생성
     */
    public static PaymentEntity createFailedPayment() {
        return createFailedPayment(FAILURE_REASON_INSUFFICIENT_BALANCE);
    }

    /**
     * 커스텀 실패 사유로 실패한 PaymentEntity 생성
     */
    public static PaymentEntity createFailedPayment(String failureReason) {
        PaymentEntity payment = createPendingPayment();
        payment.fail(failureReason);
        return payment;
    }

    /**
     * UserEntity와 PaymentCommand로 실패한 PaymentEntity 생성
     */
    public static PaymentEntity createFailedPayment(UserEntity user, PaymentCommand command, String reason) {
        return PaymentEntity.createFailed(user, command, reason);
    }

    /**
     * 타임아웃된 PaymentEntity 생성
     */
    public static PaymentEntity createTimeoutPayment() {
        PaymentEntity payment = createPendingPayment();
        payment.timeout();
        return payment;
    }

    /**
     * 취소된 PaymentEntity 생성
     */
    public static PaymentEntity createCanceledPayment() {
        PaymentEntity payment = createCompletedPayment();
        payment.cancel();
        return payment;
    }

    /**
     * 환불된 PaymentEntity 생성
     */
    public static PaymentEntity createRefundedPayment() {
        PaymentEntity payment = createCompletedPayment();
        payment.refund();
        return payment;
    }

    /**
     * Builder 패턴을 활용한 유연한 테스트 데이터 생성
     */
    public static PaymentEntityBuilder builder() {
        return new PaymentEntityBuilder();
    }

    public static class PaymentEntityBuilder {
        private Long userId = DEFAULT_USER_ID;
        private Long orderId = DEFAULT_ORDER_ID;
        private String transactionKey = DEFAULT_TRANSACTION_KEY;
        private BigDecimal amount = DEFAULT_AMOUNT;
        private final String cardType = DEFAULT_CARD_TYPE;
        private final String cardNo = DEFAULT_CARD_NO;
        private final String callbackUrl = DEFAULT_CALLBACK_URL;
        private PaymentStatus status = DEFAULT_STATUS;
        private String failureReason = null;

        public PaymentEntityBuilder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public PaymentEntityBuilder orderId(Long orderId) {
            this.orderId = orderId;
            return this;
        }

        public PaymentEntityBuilder transactionKey(String transactionKey) {
            this.transactionKey = transactionKey;
            return this;
        }

        public PaymentEntityBuilder amount(BigDecimal amount) {
            this.amount = amount;
            return this;
        }

        public PaymentEntityBuilder amount(String amount) {
            this.amount = new BigDecimal(amount);
            return this;
        }

        public PaymentEntityBuilder pending() {
            this.status = PaymentStatus.PENDING;
            this.transactionKey = null;
            return this;
        }

        public PaymentEntityBuilder completed() {
            this.status = PaymentStatus.COMPLETED;
            return this;
        }

        public PaymentEntityBuilder failed(String reason) {
            this.status = PaymentStatus.FAILED;
            this.failureReason = reason;
            return this;
        }

        public PaymentEntityBuilder largeAmount() {
            this.amount = new BigDecimal("1000000.00");
            return this;
        }

        public PaymentEntityBuilder smallAmount() {
            this.amount = new BigDecimal("1000.00");
            return this;
        }

        public PaymentEntity build() {
            PaymentDomainDtos.PaymentDomainCreateRequest request = new PaymentDomainDtos.PaymentDomainCreateRequest(
                    userId,
                    orderId,
                    transactionKey,
                    cardType,
                    cardNo,
                    callbackUrl,
                    amount,
                    status,
                    ZonedDateTime.now(),
                    failureReason
            );
            return PaymentEntity.createPayment(request);
        }
    }
}

