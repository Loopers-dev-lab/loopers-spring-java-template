package com.loopers.application.payment;

import com.loopers.domain.common.vo.Money;
import com.loopers.domain.order.InMemoryOrderRepository;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.payment.CardType;
import com.loopers.domain.payment.InMemoryPaymentRepository;
import com.loopers.domain.payment.Payment;
import com.loopers.domain.payment.PaymentStatus;
import com.loopers.domain.payment.gateway.PgGateway;
import com.loopers.domain.points.InMemoryPointRepository;
import com.loopers.domain.points.Point;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("결제 서비스 테스트")
class PaymentServiceTest {

    private PaymentService paymentService;
    private InMemoryPaymentRepository paymentRepository;
    private InMemoryOrderRepository orderRepository;
    private InMemoryPointRepository pointRepository;
    private PgGateway pgGateway;
    private ApplicationEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        paymentRepository = new InMemoryPaymentRepository();
        orderRepository = new InMemoryOrderRepository();
        pointRepository = new InMemoryPointRepository();
        pgGateway = mock(PgGateway.class);
        eventPublisher = mock(ApplicationEventPublisher.class);

        paymentService = new PaymentService(
            paymentRepository,
            orderRepository,
            pgGateway,
            pointRepository,
            eventPublisher
        );
    }

    @Test
    @DisplayName("포인트만으로 결제 완료 시 PG 호출 없이 결제 성공 처리된다")
    void 포인트_전용_결제() {
        // given
        Long memberId = 1L;

        // 주문 생성 (총액 5000원)
        OrderItem orderItem = new OrderItem(1L, 1, Money.of(5000));
        Order order = Order.create(memberId, List.of(orderItem), Money.of(5000));
        orderRepository.save(order);

        // 회원 포인트 생성 (10000원 - 충분함)
        Point memberPoint = Point.create(memberId, BigDecimal.valueOf(10000));
        pointRepository.save(memberPoint);

        PaymentCommand.RequestPayment command = new PaymentCommand.RequestPayment(
            order.getOrderNo(),
            CardType.SAMSUNG,
            "1234-5678-****-****",
            5000L,
            "http://callback.url"
        );

        // when
        PaymentInfo result = paymentService.requestPayment("user1", command);

        // then
        // 1. 포인트 차감 확인
        Point point = pointRepository.findByMemberId(memberId).orElseThrow();
        assertThat(point.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(5000));  // 10000 - 5000

        // 2. 결제 성공 확인
        assertThat(result.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(result.pointUsed()).isEqualTo(5000L);
        assertThat(result.amount()).isEqualTo(0L);  // PG 금액 0

        // 3. PG 호출 안 함
        verifyNoInteractions(pgGateway);

        // 4. 주문 상태는 이벤트 리스너가 처리하므로 여기서는 확인하지 않음
        // (통합 테스트에서 검증)
    }

    @Test
    @DisplayName("포인트 부분 사용 + PG 결제")
    void 포인트_부분_사용_PG_결제() {
        // given
        Long memberId = 1L;

        // 주문 생성 (총액 10000원)
        OrderItem orderItem = new OrderItem(1L, 1, Money.of(10000));
        Order order = Order.create(memberId, List.of(orderItem), Money.of(10000));
        orderRepository.save(order);

        // 회원 포인트 생성 (3000원 - 부족함)
        Point memberPoint = Point.create(memberId, BigDecimal.valueOf(3000));
        pointRepository.save(memberPoint);

        PaymentCommand.RequestPayment command = new PaymentCommand.RequestPayment(
            order.getOrderNo(),
            CardType.SAMSUNG,
            "1234-5678-****-****",
            10000L,
            "http://callback.url"
        );

        // PG 성공 응답 모킹
        PgGateway.PgPaymentResult pgResult = new PgGateway.PgPaymentResult(
            "TXN-KEY-123",
            PgGateway.PgTransactionStatus.SUCCESS,
            "결제 성공"
        );
        when(pgGateway.requestPayment(anyString(), any()))
            .thenReturn(java.util.concurrent.CompletableFuture.completedFuture(pgResult));

        // when
        paymentService.requestPayment("user1", command);

        // then
        // 1. 포인트 전액 차감 확인
        Point point = pointRepository.findByMemberId(memberId).orElseThrow();
        assertThat(point.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);  // 3000 - 3000

        // 2. 결제 생성 확인
        Payment payment = paymentRepository.findByOrder(order).orElseThrow();
        assertThat(payment.getPointUsed()).isEqualTo(3000L);
        assertThat(payment.getAmount()).isEqualTo(7000L);  // PG 금액 = 10000 - 3000

        // 3. PG 호출 확인
        verify(pgGateway, times(1)).requestPayment(anyString(), any());
    }

    @Test
    @DisplayName("포인트 없이 전액 PG 결제")
    void PG_전액_결제() {
        // given
        Long memberId = 1L;

        // 주문 생성 (총액 10000원)
        OrderItem orderItem = new OrderItem(1L, 1, Money.of(10000));
        Order order = Order.create(memberId, List.of(orderItem), Money.of(10000));
        orderRepository.save(order);

        // 회원 포인트 생성 (0원)
        Point memberPoint = Point.create(memberId, BigDecimal.ZERO);
        pointRepository.save(memberPoint);

        PaymentCommand.RequestPayment command = new PaymentCommand.RequestPayment(
            order.getOrderNo(),
            CardType.SAMSUNG,
            "1234-5678-****-****",
            10000L,
            "http://callback.url"
        );

        // when
        paymentService.requestPayment("user1", command);

        // then
        // 1. 포인트 변화 없음
        Point point = pointRepository.findByMemberId(memberId).orElseThrow();
        assertThat(point.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);

        // 2. 결제 생성 확인
        Payment payment = paymentRepository.findByOrder(order).orElseThrow();
        assertThat(payment.getPointUsed()).isEqualTo(0L);
        assertThat(payment.getAmount()).isEqualTo(10000L);  // PG 전액

        // 3. PG 호출 확인
        verify(pgGateway, times(1)).requestPayment(anyString(), any());
    }

    @Test
    @DisplayName("결제 실패 시 포인트가 환급된다")
    void 결제_실패시_포인트_환급() {
        // given
        Long memberId = 1L;

        // 주문 생성
        OrderItem orderItem = new OrderItem(1L, 1, Money.of(10000));
        Order order = Order.create(memberId, List.of(orderItem), Money.of(10000));
        orderRepository.save(order);

        // 회원 포인트 생성
        Point memberPoint = Point.create(memberId, BigDecimal.valueOf(10000));
        pointRepository.save(memberPoint);

        // 결제 생성 (포인트 3000원 사용)
        Payment payment = Payment.create(order, CardType.SAMSUNG, "1234-****", 7000L, 3000L, "http://callback");
        payment.assignTransactionKey("TEST-TXN-KEY-123");  // transactionKey 설정
        paymentRepository.save(payment);

        // 포인트 차감 (실제 결제 과정 시뮬레이션)
        memberPoint.pay(BigDecimal.valueOf(3000));
        pointRepository.save(memberPoint);

        PaymentCommand.ProcessCallback command = new PaymentCommand.ProcessCallback(
            payment.getTransactionKey(),
            "FAILED",
            "카드 잔액 부족"
        );

        // when
        paymentService.processCallback("user1", command);

        // then
        // 포인트 환급 확인
        Point point = pointRepository.findByMemberId(memberId).orElseThrow();
        assertThat(point.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(10000));  // 7000 + 3000
    }
}
