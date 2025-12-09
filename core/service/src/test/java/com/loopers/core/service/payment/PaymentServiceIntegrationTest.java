package com.loopers.core.service.payment;

import com.loopers.core.domain.brand.vo.BrandId;
import com.loopers.core.domain.order.Order;
import com.loopers.core.domain.order.OrderFixture;
import com.loopers.core.domain.order.OrderItemFixture;
import com.loopers.core.domain.order.repository.OrderItemRepository;
import com.loopers.core.domain.order.repository.OrderRepository;
import com.loopers.core.domain.payment.Payment;
import com.loopers.core.domain.payment.repository.PaymentRepository;
import com.loopers.core.domain.payment.type.PaymentStatus;
import com.loopers.core.domain.product.Product;
import com.loopers.core.domain.product.ProductFixture;
import com.loopers.core.domain.product.repository.ProductRepository;
import com.loopers.core.domain.product.vo.ProductName;
import com.loopers.core.domain.product.vo.ProductPrice;
import com.loopers.core.domain.product.vo.ProductStock;
import com.loopers.core.domain.user.User;
import com.loopers.core.domain.user.UserFixture;
import com.loopers.core.domain.user.repository.UserRepository;
import com.loopers.core.service.ConcurrencyTestUtil;
import com.loopers.core.service.IntegrationTest;
import com.loopers.core.service.payment.command.PaymentCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

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
                user = userRepository.save(UserFixture.createWith("user123", "user@example.com"));

                // 상품 생성 (가격: 1,500,000, 재고: 100,000개)
                product = productRepository.save(
                        ProductFixture.createWith(
                                new BrandId("1"),
                                new ProductName("MacBook Pro"),
                                new ProductPrice(new BigDecimal("1500000")),
                                new ProductStock(100_000L)
                        )
                );

                // 주문 생성
                order = orderRepository.save(OrderFixture.createWith(user.getId()));

                // 주문 상품 생성
                orderItemRepository.save(OrderItemFixture.createWith(order.getId(), product.getId(), 1L));

                // 결제 명령 생성
                paymentCommand = new PaymentCommand(
                        order.getId().value(),
                        user.getIdentifier().value(),
                        "CREDIT",
                        "1234567890123456",
                        null,
                        "CARD"
                );
            }

            @Test
            @DisplayName("하나의 결제만 생성된다")
            void 하나의_결제만_생성된다() throws InterruptedException {
                // when - 동시에 20개의 결제 요청 실행
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
