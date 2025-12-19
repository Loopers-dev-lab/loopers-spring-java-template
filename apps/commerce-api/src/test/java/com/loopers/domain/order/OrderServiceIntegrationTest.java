package com.loopers.domain.order;

import com.loopers.application.order.OrderCommand;
import com.loopers.application.order.OrderFacade;
import com.loopers.application.order.OrderInfo;
import com.loopers.application.order.OrderLineCommand;
import com.loopers.domain.common.vo.Money;
import com.loopers.domain.coupon.Coupon;
import com.loopers.domain.coupon.MemberCoupon;
import com.loopers.domain.coupon.repository.CouponRepository;
import com.loopers.domain.coupon.repository.MemberCouponRepository;
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
    private CouponRepository couponRepository;

    @Autowired
    private MemberCouponRepository memberCouponRepository;

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
            Member member = memberRepository.save(createMember("user1"));
            Long memberId = member.getId();
            Product p1 = productRepository.save(createProduct(1L, "아메리카노", 3000L, 100));
            Product p2 = productRepository.save(createProduct(1L, "라떼", 4000L, 200));

            pointRepository.save(Point.create(memberId, BigDecimal.valueOf(20000L)));

            OrderCommand command = OrderCommand.of(
                    memberId,
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

        }
    }

    @Nested
    @DisplayName("주문 실패 케이스")
    class OrderCreateFail {

        @Test
        @Transactional
        @DisplayName("재고 부족으로 실패")
        void insufficientStock_fail() {
            Member member = memberRepository.save(createMember("user1"));
            Long memberId = member.getId();
            Product item = productRepository.save(createProduct(1L, "상품", 1000L, 1));
            pointRepository.save(Point.create(memberId, BigDecimal.valueOf(5000L)));

            OrderCommand command = OrderCommand.of(
                    memberId,
                    List.of(OrderLineCommand.of(item.getId(), 5))
            );

            assertThatThrownBy(() -> orderFacade.placeOrder(command))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @Transactional
        @DisplayName("포인트 부족으로 실패")
        void insufficientPoint_fail() {
            // 주문 단계에서는 포인트를 검증하지 않는다(결제 단계에서 처리 예정)
            Member member = memberRepository.save(createMember("user1"));
            Long memberId = member.getId();
            Product item = productRepository.save(createProduct(1L, "상품", 1000L, 10));
            pointRepository.save(Point.create(memberId, BigDecimal.valueOf(2000L))); // 부족

            OrderCommand command = OrderCommand.of(
                    memberId,
                    List.of(OrderLineCommand.of(item.getId(), 5)) // 총 5000원
            );

            OrderInfo info = orderFacade.placeOrder(command);
            Order saved = orderRepository.findById(info.getId()).orElseThrow();
            assertThat(saved.getTotalPrice()).isEqualTo(Money.of(5000L));
        }

        @Test
        @Transactional
        @DisplayName("없는 상품 주문 시 실패")
        void noProduct_fail() {
            Member member = memberRepository.save(createMember("user1"));
            Long memberId = member.getId();
            pointRepository.save(Point.create(memberId, BigDecimal.valueOf(10000L)));

            OrderCommand command = OrderCommand.of(
                    memberId,
                    List.of(OrderLineCommand.of(999L, 1))
            );

            assertThatThrownBy(() -> orderFacade.placeOrder(command))
                    .isInstanceOf(RuntimeException.class);
        }

        @Test
        @Transactional
        @DisplayName("유저 포인트 정보 없어도 주문 생성은 성공 (결제는 별도 단계)")
        void noUserPoint_orderCreationSucceeds() {
            Member member = memberRepository.save(createMember("user1"));
            Long memberId = member.getId();
            Product item = productRepository.save(createProduct(1L, "상품", 1000L, 10));

            OrderCommand command = OrderCommand.of(
                    memberId,
                    List.of(OrderLineCommand.of(item.getId(), 1))
            );

            // 주문 생성은 성공해야 함 (포인트 체크는 결제 단계에서)
            OrderInfo result = orderFacade.placeOrder(command);

            assertThat(result).isNotNull();
            assertThat(result.getOrderNo()).isNotNull();
        }
    }

    @Nested
    @DisplayName("쿠폰 적용 주문")
    class OrderWithCoupon {

        @Test
        @DisplayName("정액 쿠폰 적용 시 할인된 금액으로 결제된다")
        void fixedCoupon_success() {
            // given
            Member member = memberRepository.save(createMember("user1"));
            Long memberId = member.getId();
            Product item = productRepository.save(createProduct(1L, "상품", 10000L, 10));
            pointRepository.save(Point.create(memberId, BigDecimal.valueOf(20000L)));

            Coupon coupon = couponRepository.save(Coupon.createFixedCoupon("1000원 할인", BigDecimal.valueOf(1000)));
            MemberCoupon memberCoupon = memberCouponRepository.save(MemberCoupon.issue(memberId, coupon));

            OrderCommand command = OrderCommand.of(
                    memberId,
                    List.of(OrderLineCommand.of(item.getId(), 1)),
                    memberCoupon.getId()
            );

            // when
            OrderInfo info = orderFacade.placeOrder(command);

            // then
            Order saved = orderRepository.findById(info.getId()).orElseThrow();
            assertThat(saved.getTotalPrice()).isEqualTo(Money.of(9000L)); // 10000 - 1000

            // 쿠폰 사용 처리 확인
            MemberCoupon usedCoupon = memberCouponRepository.findById(memberCoupon.getId()).orElseThrow();
            assertThat(usedCoupon.isUsed()).isTrue();

        }

        @Test
        @DisplayName("정률 쿠폰 적용 시 할인된 금액으로 결제된다")
        void percentageCoupon_success() {
            // given
            Member member = memberRepository.save(createMember("user1"));
            Long memberId = member.getId();
            Product item = productRepository.save(createProduct(1L, "상품", 10000L, 10));
            pointRepository.save(Point.create(memberId, BigDecimal.valueOf(20000L)));

            Coupon coupon = couponRepository.save(Coupon.createPercentageCoupon("10% 할인", BigDecimal.valueOf(10)));
            MemberCoupon memberCoupon = memberCouponRepository.save(MemberCoupon.issue(memberId, coupon));

            OrderCommand command = OrderCommand.of(
                    memberId,
                    List.of(OrderLineCommand.of(item.getId(), 1)),
                    memberCoupon.getId()
            );

            // when
            OrderInfo info = orderFacade.placeOrder(command);

            // then
            Order saved = orderRepository.findById(info.getId()).orElseThrow();
            assertThat(saved.getTotalPrice()).isEqualTo(Money.of(9000L)); // 10000 * 0.9 = 9000

        }

        @Test
        @DisplayName("존재하지 않는 쿠폰으로 주문 시 실패한다")
        void nonExistentCoupon_fail() {
            // given
            Member member = memberRepository.save(createMember("user1"));
            Long memberId = member.getId();
            Product item = productRepository.save(createProduct(1L, "상품", 10000L, 10));
            pointRepository.save(Point.create(memberId, BigDecimal.valueOf(20000L)));

            OrderCommand command = OrderCommand.of(
                    memberId,
                    List.of(OrderLineCommand.of(item.getId(), 1)),
                    999L // 존재하지 않는 쿠폰 ID
            );

            // when & then
            assertThatThrownBy(() -> orderFacade.placeOrder(command))
                    .hasMessageContaining("쿠폰을 찾을 수 없습니다");
        }

        @Test
        @DisplayName("이미 사용된 쿠폰으로 주문 시 실패한다")
        void alreadyUsedCoupon_fail() {
            // given
            Member member = memberRepository.save(createMember("user1"));
            Long memberId = member.getId();
            Product item = productRepository.save(createProduct(1L, "상품", 10000L, 10));
            pointRepository.save(Point.create(memberId, BigDecimal.valueOf(20000L)));

            Coupon coupon = couponRepository.save(Coupon.createFixedCoupon("1000원 할인", BigDecimal.valueOf(1000)));
            MemberCoupon memberCoupon = MemberCoupon.issue(memberId, coupon);
            memberCoupon.use(); // 이미 사용 처리
            memberCouponRepository.save(memberCoupon);

            OrderCommand command = OrderCommand.of(
                    memberId,
                    List.of(OrderLineCommand.of(item.getId(), 1)),
                    memberCoupon.getId()
            );

            // when & then
            assertThatThrownBy(() -> orderFacade.placeOrder(command))
                    .hasMessageContaining("사용할 수 없는 쿠폰입니다");
        }

        @Test
        @DisplayName("다른 회원의 쿠폰으로 주문 시 실패한다")
        void otherMemberCoupon_fail() {
            // given
            Member member1 = memberRepository.save(createMember("user1"));
            Long memberId1 = member1.getId();
            Member member2 = memberRepository.save(createMember("user2"));
            Long memberId2 = member2.getId();
            Product item = productRepository.save(createProduct(1L, "상품", 10000L, 10));
            pointRepository.save(Point.create(memberId1, BigDecimal.valueOf(20000L)));

            Coupon coupon = couponRepository.save(Coupon.createFixedCoupon("1000원 할인", BigDecimal.valueOf(1000)));
            MemberCoupon memberCoupon = memberCouponRepository.save(MemberCoupon.issue(memberId2, coupon)); // user2의 쿠폰

            OrderCommand command = OrderCommand.of(
                    memberId1, // user1이 user2의 쿠폰 사용 시도
                    List.of(OrderLineCommand.of(item.getId(), 1)),
                    memberCoupon.getId()
            );

            // when & then
            assertThatThrownBy(() -> orderFacade.placeOrder(command))
                    .hasMessageContaining("본인의 쿠폰만 사용할 수 있습니다");
        }

        @Test
        @DisplayName("할인 금액이 주문 금액보다 클 경우 0원으로 결제된다")
        void discountExceedsPrice_zeroPayment() {
            // given
            Member member = memberRepository.save(createMember("user1"));
            Long memberId = member.getId();
            Product item = productRepository.save(createProduct(1L, "상품", 1000L, 10));
            pointRepository.save(Point.create(memberId, BigDecimal.valueOf(20000L)));

            Coupon coupon = couponRepository.save(Coupon.createFixedCoupon("5000원 할인", BigDecimal.valueOf(5000)));
            MemberCoupon memberCoupon = memberCouponRepository.save(MemberCoupon.issue(memberId, coupon));

            OrderCommand command = OrderCommand.of(
                    memberId,
                    List.of(OrderLineCommand.of(item.getId(), 1)), // 1000원 상품
                    memberCoupon.getId()
            );

            // when
            OrderInfo info = orderFacade.placeOrder(command);

            // then
            Order saved = orderRepository.findById(info.getId()).orElseThrow();
            assertThat(saved.getTotalPrice()).isEqualTo(Money.of(0L)); // 1000 - 1000(최대 할인) = 0

        }

        @Test
        @DisplayName("쿠폰 적용 주문 실패 시 쿠폰 상태가 롤백된다")
        void couponRollback_onOrderFailure() {
            // given
            Member member = memberRepository.save(createMember("user1"));
            Long memberId = member.getId();
            Product item = productRepository.save(createProduct(1L, "상품", 10000L, 1)); // 재고 1개
            pointRepository.save(Point.create(memberId, BigDecimal.valueOf(20000L)));

            Coupon coupon = couponRepository.save(Coupon.createFixedCoupon("1000원 할인", BigDecimal.valueOf(1000)));
            MemberCoupon memberCoupon = memberCouponRepository.save(MemberCoupon.issue(memberId, coupon));

            OrderCommand command = OrderCommand.of(
                    memberId,
                    List.of(OrderLineCommand.of(item.getId(), 5)), // 재고 초과
                    memberCoupon.getId()
            );

            // when
            assertThatThrownBy(() -> orderFacade.placeOrder(command))
                    .isInstanceOf(RuntimeException.class);

            // then - 쿠폰은 사용되지 않은 상태로 유지
            MemberCoupon notUsedCoupon = memberCouponRepository.findById(memberCoupon.getId()).orElseThrow();
            assertThat(notUsedCoupon.isUsed()).isFalse();
        }
    }
}
