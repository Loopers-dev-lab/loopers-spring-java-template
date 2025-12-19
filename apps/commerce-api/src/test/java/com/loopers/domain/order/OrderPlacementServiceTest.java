package com.loopers.domain.order;

import com.loopers.domain.common.vo.Money;
import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.InMemoryMemberCouponRepository;
import com.loopers.domain.coupon.MemberCoupon;
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
import com.loopers.support.TestEntityUtils;
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
    private InMemoryMemberCouponRepository memberCouponRepository;
    private OrderPlacementService orderPlacementService;
    private long memberSequence;

    @BeforeEach
    void setUp() {
        orderRepository = new InMemoryOrderRepository();
        productRepository = new InMemoryProductRepository();
        memberRepository = new InMemoryMemberRepository();
        pointRepository = new InMemoryPointRepository();
        memberCouponRepository = new InMemoryMemberCouponRepository();
        memberSequence = 0L;
        orderPlacementService = new OrderPlacementService(
                orderRepository,
                productRepository,
                memberRepository,
                memberCouponRepository
        );
    }

    @DisplayName("주문 처리")
    @Nested
    class PlaceOrder {
        @DisplayName("정상적인 주문이 성공적으로 처리된다")
        @Test
        void shouldProcessOrder_whenValidOrderPlaced() {
            // given
            Long memberId = setupMemberWithPoints("member1", BigDecimal.valueOf(50000));

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
        }

        @DisplayName("재고가 부족하면 예외가 발생한다")
        @Test
        void shouldThrowException_whenInsufficientStock() {
            // given
            Long memberId = setupMemberWithPoints("member1", BigDecimal.valueOf(50000));

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

        @DisplayName("존재하지 않는 회원이 주문하면 예외가 발생한다")
        @Test
        void shouldThrowException_whenMemberNotFound() {
            // given
            Long nonExistentMemberId = 999L; // ID that doesn't exist
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
            Long memberId = setupMemberWithPoints("member1", BigDecimal.valueOf(50000));

            OrderPlacementCommand command = OrderPlacementCommand.of(
                    memberId,
                    List.of(OrderLineCommand.of(999L, 1)) // 존재하지 않는 상품 ID
            );

            // when & then
            assertThatThrownBy(() -> orderPlacementService.placeOrder(command))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("상품을 찾을 수 없습니다");
        }

        // 포인트 차감/부족 검증은 결제 단계에서 수행하므로 이 레이어에서는 다루지 않는다.
    }

    @DisplayName("계산 정확성")
    @Nested
    class CalculationAccuracy {
    
        @DisplayName("여러 상품 주문시 총액이 정확히 계산된다")
        @Test
        void shouldCalculateTotalCorrectly_whenOrderingMultipleProducts() {
        // given
        Long memberId = setupMemberWithPoints("member1", BigDecimal.valueOf(100000));
        
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

    @DisplayName("쿠폰 처리")
    @Nested
    class CouponHandling {

        @DisplayName("쿠폰 적용 시 할인된 금액으로 주문이 생성된다")
        @Test
        void shouldApplyDiscount_whenCouponProvided() {
            // given
            Long memberId = setupMemberWithPoints("member1", BigDecimal.valueOf(50000));
            setupProduct(1L, Money.of(10000), Stock.of(100));

            // 1000원 할인 쿠폰
            Coupon coupon = Coupon.createFixedCoupon("1000원 쿠폰", BigDecimal.valueOf(1000));
            MemberCoupon memberCoupon = MemberCoupon.issue(memberId, coupon);
            memberCouponRepository.save(memberCoupon);

            OrderPlacementCommand command = OrderPlacementCommand.of(
                    memberId,
                    List.of(OrderLineCommand.of(1L, 1)),
                    memberCoupon.getId()
            );

            // when
            Order result = orderPlacementService.placeOrder(command);

            // then
            assertThat(result.getTotalPrice()).isEqualTo(Money.of(9000));  // 10000 - 1000

            // 쿠폰은 주문 생성 시 사용됨 (동시성 보장을 위해)
            MemberCoupon savedCoupon = memberCouponRepository.findById(memberCoupon.getId()).orElseThrow();
            assertThat(savedCoupon.isUsed()).isTrue();  // 사용됨 상태
        }

        @DisplayName("다른 회원의 쿠폰 사용 시 예외가 발생한다")
        @Test
        void shouldThrowException_whenUsingOtherMemberCoupon() {
            // given
            Long memberId = setupMemberWithPoints("member1", BigDecimal.valueOf(50000));
            Long otherMemberId = setupMemberWithPoints("member2", BigDecimal.valueOf(50000));
            setupProduct(1L, Money.of(10000), Stock.of(100));

            // 다른 회원의 쿠폰
            Coupon coupon = Coupon.createFixedCoupon("1000원 쿠폰", BigDecimal.valueOf(1000));
            MemberCoupon otherMemberCoupon = MemberCoupon.issue(otherMemberId, coupon);
            memberCouponRepository.save(otherMemberCoupon);

            OrderPlacementCommand command = OrderPlacementCommand.of(
                    memberId,  // member1
                    List.of(OrderLineCommand.of(1L, 1)),
                    otherMemberCoupon.getId()  // member2의 쿠폰
            );

            // when & then
            assertThatThrownBy(() -> orderPlacementService.placeOrder(command))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("본인의 쿠폰만 사용할 수 있습니다");
        }

        @DisplayName("이미 사용된 쿠폰 사용 시 예외가 발생한다")
        @Test
        void shouldThrowException_whenCouponAlreadyUsed() {
            // given
            Long memberId = setupMemberWithPoints("member1", BigDecimal.valueOf(50000));
            setupProduct(1L, Money.of(10000), Stock.of(100));

            Coupon coupon = Coupon.createFixedCoupon("1000원 쿠폰", BigDecimal.valueOf(1000));
            MemberCoupon memberCoupon = MemberCoupon.issue(memberId, coupon);
            memberCoupon.use();  // 쿠폰 미리 사용
            memberCouponRepository.save(memberCoupon);

            OrderPlacementCommand command = OrderPlacementCommand.of(
                    memberId,
                    List.of(OrderLineCommand.of(1L, 1)),
                    memberCoupon.getId()
            );

            // when & then
            assertThatThrownBy(() -> orderPlacementService.placeOrder(command))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("사용할 수 없는 쿠폰입니다");
        }
    }

    private Long setupMemberWithPoints(String username, BigDecimal points) {
        Member member = new Member(
                username,
                username + "@test.com",
                "password123",
                "1990-01-01",
                Gender.MALE
        );
        Member saved = TestEntityUtils.setIdWithNow(member, ++memberSequence);
        saved = memberRepository.save(saved);
        Long memberId = saved.getId();
        pointRepository.save(Point.create(memberId, points));
        return memberId;
    }

    private Long setupMember(String username) {
        Member member = new Member(
                username,
                username + "@test.com",
                "password123",
                "1990-01-01",
                Gender.MALE
        );
        Member saved = TestEntityUtils.setIdWithNow(member, ++memberSequence);
        saved = memberRepository.save(saved);
        return saved.getId();
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
