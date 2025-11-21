package com.loopers.domain.order;

import com.loopers.domain.common.vo.Money;
import com.loopers.domain.members.InMemoryMemberRepository;
import com.loopers.domain.members.enums.Gender;
import com.loopers.domain.members.Member;
import com.loopers.domain.order.command.OrderLineCommand;
import com.loopers.domain.order.command.OrderPlacementCommand;
import com.loopers.domain.order.service.OrderPlacementService;
import com.loopers.domain.points.InMemoryPointRepository;
import com.loopers.domain.points.Point;
import com.loopers.domain.product.InMemoryProductRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.vo.Stock;
import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class OrderPlacementServiceTest {

    private InMemoryOrderRepository orderRepository;
    private InMemoryProductRepository productRepository;
    private InMemoryMemberRepository memberRepository;
    private InMemoryPointRepository pointRepository;
    private OrderPlacementService orderPlacementService;

    @BeforeEach
    void setUp() {
        orderRepository = new InMemoryOrderRepository();
        productRepository = new InMemoryProductRepository();
        memberRepository = new InMemoryMemberRepository();
        pointRepository = new InMemoryPointRepository();
        orderPlacementService = new OrderPlacementService(
                orderRepository,
                productRepository,
                memberRepository,
                pointRepository
        );
    }

    @DisplayName("주문 처리")
    @Nested
    class PlaceOrder {
    
        @DisplayName("정상적인 주문이 성공적으로 처리된다")
        @Test
        void shouldProcessOrder_whenValidOrderPlaced() {
        // given
        String memberId = "member1";
        setupMemberWithPoints(memberId, BigDecimal.valueOf(50000));
        
        Product product1 = setupProduct(1L, Money.of(10000), Stock.of(100));
        Product product2 = setupProduct(2L, Money.of(15000), Stock.of(50));

        OrderPlacementCommand command = OrderPlacementCommand.of(
                memberId,
                List.of(
                        OrderLineCommand.of(1L, 2), // 10000 * 2 = 20000
                        OrderLineCommand.of(2L, 1)  // 15000 * 1 = 15000
                )
        );

        // when
        Order result = orderPlacementService.placeOrder(command);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getMemberId()).isEqualTo(memberId);
        assertThat(result.getTotalPrice()).isEqualTo(Money.of(35000));
        assertThat(result.getItems()).hasSize(2);

        // 재고 차감 확인
        assertThat(product1.getStock().getQuantity()).isEqualTo(98);
        assertThat(product2.getStock().getQuantity()).isEqualTo(49);

        // 포인트 차감 확인
        Point memberPoints = pointRepository.findByMemberId(memberId).orElseThrow();
        assertThat(memberPoints.getAmount()).isEqualTo(BigDecimal.valueOf(15000)); // 50000 - 35000
    }

        @DisplayName("재고가 부족하면 예외가 발생한다")
        @Test
        void shouldThrowException_whenInsufficientStock() {
        // given
        String memberId = "member1";
        setupMemberWithPoints(memberId, BigDecimal.valueOf(50000));
        
        Product product = setupProduct(1L, Money.of(10000), Stock.of(5));

        OrderPlacementCommand command = OrderPlacementCommand.of(
                memberId,
                List.of(OrderLineCommand.of(1L, 10)) // 재고(5)보다 많은 수량(10) 주문
        );

        // when & then
        assertThatThrownBy(() -> orderPlacementService.placeOrder(command))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining("재고가 부족합니다");

        // 재고는 변경되지 않아야 함
        assertThat(product.getStock().getQuantity()).isEqualTo(5);
    }

        @DisplayName("포인트가 부족하면 예외가 발생한다")
        @Test
        void shouldThrowException_whenInsufficientPoints() {
        // given
        String memberId = "member1";
        setupMemberWithPoints(memberId, BigDecimal.valueOf(5000)); // 부족한 포인트
        
        setupProduct(1L, Money.of(10000), Stock.of(100));

        OrderPlacementCommand command = OrderPlacementCommand.of(
                memberId,
                List.of(OrderLineCommand.of(1L, 1)) // 10000원 상품, 포인트는 5000원만 있음
        );

        // when & then
        assertThatThrownBy(() -> orderPlacementService.placeOrder(command))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining("포인트가 부족합니다");
    }

        @DisplayName("존재하지 않는 회원이 주문하면 예외가 발생한다")
        @Test
        void shouldThrowException_whenMemberNotFound() {
        // given
        String nonExistentMemberId = "none123";
        setupProduct(1L, Money.of(10000), Stock.of(100));

        OrderPlacementCommand command = OrderPlacementCommand.of(
                nonExistentMemberId,
                List.of(OrderLineCommand.of(1L, 1))
        );

        // when & then
        assertThatThrownBy(() -> orderPlacementService.placeOrder(command))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining("회원을 찾을 수 없습니다");
    }

        @DisplayName("존재하지 않는 상품을 주문하면 예외가 발생한다")
        @Test
        void shouldThrowException_whenProductNotFound() {
        // given
        String memberId = "member1";
        setupMemberWithPoints(memberId, BigDecimal.valueOf(50000));

        OrderPlacementCommand command = OrderPlacementCommand.of(
                memberId,
                List.of(OrderLineCommand.of(999L, 1)) // 존재하지 않는 상품 ID
        );

        // when & then
        assertThatThrownBy(() -> orderPlacementService.placeOrder(command))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining("상품을 찾을 수 없습니다");
    }

        @DisplayName("포인트 정보가 없는 회원이 주문하면 예외가 발생한다")
        @Test
        void shouldThrowException_whenMemberPointsNotFound() {
        // given
        String memberId = "member1";
        setupMember(memberId); // 포인트는 설정하지 않음
        setupProduct(1L, Money.of(10000), Stock.of(100));

        OrderPlacementCommand command = OrderPlacementCommand.of(
                memberId,
                List.of(OrderLineCommand.of(1L, 1))
        );

        // when & then
        assertThatThrownBy(() -> orderPlacementService.placeOrder(command))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining("포인트 정보를 찾을 수 없습니다");
        }
    }

    @DisplayName("계산 정확성")
    @Nested
    class CalculationAccuracy {
    
        @DisplayName("여러 상품 주문시 총액이 정확히 계산된다")
        @Test
        void shouldCalculateTotalCorrectly_whenOrderingMultipleProducts() {
        // given
        String memberId = "member1";
        setupMemberWithPoints(memberId, BigDecimal.valueOf(100000));
        
        setupProduct(1L, Money.of(10000), Stock.of(100));
        setupProduct(2L, Money.of(25000), Stock.of(50));
        setupProduct(3L, Money.of(5000), Stock.of(200));

        OrderPlacementCommand command = OrderPlacementCommand.of(
                memberId,
                List.of(
                        OrderLineCommand.of(1L, 3), // 10000 * 3 = 30000
                        OrderLineCommand.of(2L, 2), // 25000 * 2 = 50000  
                        OrderLineCommand.of(3L, 1)  // 5000 * 1 = 5000
                )
        );

        // when
        Order result = orderPlacementService.placeOrder(command);

        // then
        assertThat(result.getTotalPrice()).isEqualTo(Money.of(85000));
        assertThat(result.getItems()).hasSize(3);
        
        // 각 주문 항목의 가격 확인
        assertThat(result.getItems().get(0).getTotalPrice()).isEqualTo(Money.of(30000));
        assertThat(result.getItems().get(1).getTotalPrice()).isEqualTo(Money.of(50000));
        assertThat(result.getItems().get(2).getTotalPrice()).isEqualTo(Money.of(5000));
        }
    }

    private void setupMemberWithPoints(String memberId, BigDecimal points) {
        setupMember(memberId);
        pointRepository.save(Point.create(memberId, points));
    }

    private void setupMember(String memberId) {
        Member member = new Member(
                memberId,
                memberId + "@test.com",
                "password123",
                "1990-01-01",
                Gender.MALE
        );
        memberRepository.save(member);
    }

    private Product setupProduct(Long productId, Money price, Stock stock) {
        Product product = new Product(
                1L, // brandId
                "테스트 상품 " + productId,
                "상품 설명",
                price,
                stock
        );
        return productRepository.saveWithId(productId, product);
    }
}
