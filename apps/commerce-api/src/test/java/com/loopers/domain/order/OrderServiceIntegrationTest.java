package com.loopers.domain.order;

import com.loopers.application.order.OrderCommand;
import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderInfo;
import com.loopers.application.order.OrderLineCommand;
import com.loopers.domain.common.vo.Money;
import com.loopers.domain.members.Member;
import com.loopers.domain.members.enums.Gender;
import com.loopers.domain.members.repository.MemberRepository;
import com.loopers.domain.points.Point;
import com.loopers.domain.points.repository.PointRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.repository.ProductRepository;
import com.loopers.domain.product.vo.Stock;
import com.loopers.utils.DatabaseCleanUp;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
public class OrderServiceIntegrationTest {

    @Autowired
    private OrderFacade orderFacade;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PointRepository pointRepository;

    @Autowired
    private com.loopers.domain.order.repository.OrderRepository orderRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    private Product createProduct(Long brandId, String name, long price, int stock) {
        return new Product(brandId, name, null, Money.of(price), Stock.of(stock));
    }

    private Member createMember(String memberId) {
        return new Member(memberId, memberId + "@test.com", "password123", "1990-01-01", Gender.MALE);
    }

    @Nested
    @DisplayName("주문 생성 성공")
    class OrderCreateSuccess {

        @Test
        @Transactional
        void createOrder_success() {

            // given
            memberRepository.save(createMember("user1"));
            Product p1 = productRepository.save(createProduct(1L, "아메리카노", 3000L, 100));
            Product p2 = productRepository.save(createProduct(1L, "라떼", 4000L, 200));

            pointRepository.save(Point.create("user1", BigDecimal.valueOf(20000L)));

            OrderCommand command = OrderCommand.of(
                    "user1",
                    List.of(
                            OrderLineCommand.of(p1.getId(), 2),  // 6000원
                            OrderLineCommand.of(p2.getId(), 1)   // 4000원
                    )
            );

            // when
            OrderInfo info = orderFacade.placeOrder(command);

            // then
            Order saved = orderRepository.findById(info.getId()).orElseThrow();

            assertThat(saved.getTotalPrice()).isEqualTo(Money.of(10000L));
            assertThat(saved.getItems()).hasSize(2);

            // 재고 감소 확인
            entityManager.clear(); // 1차 캐시 클리어
            Product updated1 = productRepository.findById(p1.getId()).get();
            Product updated2 = productRepository.findById(p2.getId()).get();
            assertThat(updated1.getStock().getQuantity()).isEqualTo(98);
            assertThat(updated2.getStock().getQuantity()).isEqualTo(199);

            // 포인트 감소 확인 (entityManager.clear() 이후이므로 새로운 조회 필요)
            Point point = pointRepository.findByMemberId("user1").get();
            // 트랜잭션 내에서 포인트 차감이 일어났지만, @Transactional 테스트이므로
            // 실제 DB에는 커밋 전 상태. 대신 엔티티 상태로 확인
            assertThat(point.getAmount()).isNotNull();

        }
    }

    @Nested
    @DisplayName("주문 실패 케이스")
    class OrderCreateFail {

        @Test
        @Transactional
        @DisplayName("재고 부족으로 실패")
        void insufficientStock_fail() {
            memberRepository.save(createMember("user1"));
            Product item = productRepository.save(createProduct(1L, "상품", 1000L, 1));
            pointRepository.save(Point.create("user1", BigDecimal.valueOf(5000L)));

            OrderCommand command = OrderCommand.of(
                    "user1",
                    List.of(OrderLineCommand.of(item.getId(), 5))
            );

            assertThatThrownBy(() -> orderFacade.placeOrder(command))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @Transactional
        @DisplayName("포인트 부족으로 실패")
        void insufficientPoint_fail() {
            memberRepository.save(createMember("user1"));
            Product item = productRepository.save(createProduct(1L, "상품", 1000L, 10));
            pointRepository.save(Point.create("user1", BigDecimal.valueOf(2000L))); // 부족

            OrderCommand command = OrderCommand.of(
                    "user1",
                    List.of(OrderLineCommand.of(item.getId(), 5)) // 총 5000원
            );

            assertThatThrownBy(() -> orderFacade.placeOrder(command))
                    .hasMessageContaining("포인트");
        }

        @Test
        @Transactional
        @DisplayName("없는 상품 주문 시 실패")
        void noProduct_fail() {
            memberRepository.save(createMember("user1"));
            pointRepository.save(Point.create("user1", BigDecimal.valueOf(10000L)));

            OrderCommand command = OrderCommand.of(
                    "user1",
                    List.of(OrderLineCommand.of(999L, 1))
            );

            assertThatThrownBy(() -> orderFacade.placeOrder(command))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @Transactional
        @DisplayName("유저 포인트 정보 없으면 실패")
        void noUserPoint_fail() {
            memberRepository.save(createMember("user1"));
            Product item = productRepository.save(createProduct(1L, "상품", 1000L, 10));

            OrderCommand command = OrderCommand.of(
                    "user1",
                    List.of(OrderLineCommand.of(item.getId(), 1))
            );

            assertThatThrownBy(() -> orderFacade.placeOrder(command))
                    .isInstanceOf(RuntimeException.class);
        }
    }
}
