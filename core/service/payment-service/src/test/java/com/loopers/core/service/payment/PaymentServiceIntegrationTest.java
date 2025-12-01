package com.loopers.core.service.payment;

import com.loopers.core.domain.brand.vo.BrandId;
import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.DeletedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.order.Order;
import com.loopers.core.domain.order.OrderItem;
import com.loopers.core.domain.order.repository.OrderItemRepository;
import com.loopers.core.domain.order.repository.OrderRepository;
import com.loopers.core.domain.order.vo.OrderId;
import com.loopers.core.domain.order.vo.OrderItemId;
import com.loopers.core.domain.order.vo.Quantity;
import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.PgClient;
import com.loopers.core.domain.payment.PgPayment;
import com.loopers.core.domain.payment.repository.PaymentRepository;
import com.loopers.core.domain.payment.type.PaymentStatus;
import com.loopers.core.domain.payment.vo.FailedReason;
import com.loopers.core.domain.payment.vo.TransactionKey;
import com.loopers.core.domain.product.Product;
import com.loopers.core.domain.product.repository.ProductRepository;
import com.loopers.core.domain.product.vo.*;
import com.loopers.core.domain.user.User;
import com.loopers.core.domain.user.repository.UserRepository;
import com.loopers.core.domain.user.vo.UserEmail;
import com.loopers.core.domain.user.vo.UserId;
import com.loopers.core.domain.user.vo.UserIdentifier;
import com.loopers.core.service.ConcurrencyTestUtil;
import com.loopers.core.service.IntegrationTest;
import com.loopers.core.service.payment.command.PaymentCommand;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.instancio.Select.field;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@DisplayName("결제 서비스")
class PaymentServiceIntegrationTest extends IntegrationTest {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockitoBean
    private PgClient pgClient;

    @Nested
    @DisplayName("pay() 메서드")
    class PayMethod {

        @Nested
        @DisplayName("동시에 같은 주문에 대해 결제 요청을 하면")
        class 동시_결제_요청 {

            private Order order;
            private User user;
            private Product product;
            private PaymentCommand paymentCommand;

            @BeforeEach
            void setUp() {
                // 사용자 생성
                user = userRepository.save(
                        Instancio.of(User.class)
                                .set(field(User::getId), UserId.empty())
                                .set(field(User::getIdentifier), UserIdentifier.create("user123"))
                                .set(field(User::getEmail), new UserEmail("user@example.com"))
                                .create()
                );

                // 상품 생성 (가격: 10,000, 재고: 100개)
                product = productRepository.save(
                        Instancio.of(Product.class)
                                .set(field(Product::getId), ProductId.empty())
                                .set(field(Product::getBrandId), new BrandId("1"))
                                .set(field(Product::getName), new ProductName("MacBook Pro"))
                                .set(field(Product::getPrice), new ProductPrice(new BigDecimal("1500000")))
                                .set(field(Product::getStock), new ProductStock(100_000L))
                                .set(field(Product::getLikeCount), ProductLikeCount.init())
                                .create()
                );

                // 주문 생성
                order = orderRepository.save(
                        Instancio.of(Order.class)
                                .set(field(Order::getId), OrderId.empty())
                                .set(field(Order::getUserId), user.getId())
                                .set(field(Order::getCreatedAt), CreatedAt.now())
                                .set(field(Order::getUpdatedAt), UpdatedAt.now())
                                .set(field(Order::getDeletedAt), DeletedAt.empty())
                                .create()
                );

                // 주문 상품 생성
                orderItemRepository.save(
                        Instancio.of(OrderItem.class)
                                .set(field(OrderItem::getId), OrderItemId.empty())
                                .set(field(OrderItem::getOrderId), order.getId())
                                .set(field(OrderItem::getProductId), product.getId())
                                .set(field(OrderItem::getQuantity), new Quantity(1L))
                                .create()
                );

                // 결제 명령 생성
                paymentCommand = new PaymentCommand(
                        order.getId().value(),
                        user.getIdentifier().value(),
                        "CREDIT",
                        "1234567890123456",
                        null
                );

                // PG 클라이언트 Mock 설정
                when(pgClient.pay(any(Payment.class), anyString()))
                        .thenReturn(new PgPayment(new TransactionKey("TXN_" + System.nanoTime()), PaymentStatus.PENDING, FailedReason.empty()));
            }

            @Test
            @DisplayName("하나의 결제만 생성된다")
            void 하나의_결제만_생성된다() throws InterruptedException {
                // when - 동시에 10개의 결제 요청 실행
                List<Payment> results = ConcurrencyTestUtil.executeInParallel(
                        20,
                        index -> paymentService.pay(paymentCommand)
                );

                // then - 성공한 결제 1개만 반환되어야 함
                assertSoftly(softly -> {
                    softly.assertThat(results)
                            .as("성공한 결제 개수")
                            .hasSize(1);
                    boolean paymentExists = paymentRepository.findBy(order.getOrderKey(), PaymentStatus.PENDING).isPresent();
                    softly.assertThat(paymentExists)
                            .as("주문의 결제 상태 존재")
                            .isTrue();
                });
            }
        }
    }
}
