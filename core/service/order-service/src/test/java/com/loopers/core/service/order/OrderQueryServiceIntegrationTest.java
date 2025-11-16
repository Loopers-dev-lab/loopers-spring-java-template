package com.loopers.core.service.order;

import com.loopers.core.domain.brand.Brand;
import com.loopers.core.domain.brand.repository.BrandRepository;
import com.loopers.core.domain.brand.vo.BrandDescription;
import com.loopers.core.domain.brand.vo.BrandId;
import com.loopers.core.domain.brand.vo.BrandName;
import com.loopers.core.domain.error.NotFoundException;
import com.loopers.core.domain.order.Order;
import com.loopers.core.domain.order.OrderDetail;
import com.loopers.core.domain.order.OrderItem;
import com.loopers.core.domain.order.OrderListView;
import com.loopers.core.domain.order.repository.OrderItemRepository;
import com.loopers.core.domain.order.repository.OrderRepository;
import com.loopers.core.domain.order.vo.OrderId;
import com.loopers.core.domain.order.vo.Quantity;
import com.loopers.core.domain.product.Product;
import com.loopers.core.domain.product.repository.ProductRepository;
import com.loopers.core.domain.product.vo.ProductName;
import com.loopers.core.domain.product.vo.ProductPrice;
import com.loopers.core.domain.product.vo.ProductStock;
import com.loopers.core.domain.user.User;
import com.loopers.core.domain.user.repository.UserRepository;
import com.loopers.core.domain.user.type.UserGender;
import com.loopers.core.domain.user.vo.UserBirthDay;
import com.loopers.core.domain.user.vo.UserEmail;
import com.loopers.core.domain.user.vo.UserId;
import com.loopers.core.domain.user.vo.UserIdentifier;
import com.loopers.core.service.IntegrationTest;
import com.loopers.core.service.order.query.GetOrderDetailQuery;
import com.loopers.core.service.order.query.GetOrderListQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class OrderQueryServiceIntegrationTest extends IntegrationTest {

    @Autowired
    private OrderQueryService orderQueryService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ProductRepository productRepository;

    @Nested
    @DisplayName("주문 리스트 조회 시")
    class 주문_리스트_조회_시 {

        private String savedUserIdentifier;
        private UserId savedUserId;
        private UserId otherUserId;

        @BeforeEach
        void setUp() {
            User user = userRepository.save(User.create(
                    UserIdentifier.create("loopers"),
                    new UserEmail("loopers@test.com"),
                    UserBirthDay.create("1990-01-01"),
                    UserGender.create("MALE")
            ));
            savedUserIdentifier = user.getIdentifier().value();
            savedUserId = user.getUserId();

            User otherUser = userRepository.save(User.create(
                    UserIdentifier.create("other"),
                    new UserEmail("other@test.com"),
                    UserBirthDay.create("1991-02-02"),
                    UserGender.create("FEMALE")
            ));
            otherUserId = otherUser.getUserId();
        }

        @Nested
        @DisplayName("조건에 맞는 주문이 존재하는 경우")
        class 조건에_맞는_주문이_존재하는_경우 {

            @BeforeEach
            void setUp() {
                orderRepository.save(Order.create(savedUserId));
                orderRepository.save(Order.create(savedUserId));
                orderRepository.save(Order.create(savedUserId));
            }

            @Test
            @DisplayName("주문 리스트가 조회된다.")
            void 주문_리스트가_조회된다() {
                GetOrderListQuery query = new GetOrderListQuery(
                        savedUserIdentifier,
                        "ASC",
                        0,
                        10
                );

                OrderListView result = orderQueryService.getOrderListWithCondition(query);

                assertSoftly(softly -> {
                    softly.assertThat(result).isNotNull();
                    softly.assertThat(result.getItems()).isNotEmpty();
                    softly.assertThat(result.getTotalElements()).isEqualTo(3);
                });
            }

            @Test
            @DisplayName("생성일시 오름차순으로 정렬된다.")
            void 생성일시_오름차순으로_정렬된다() {
                GetOrderListQuery query = new GetOrderListQuery(
                        savedUserIdentifier,
                        "ASC",
                        0,
                        10
                );

                OrderListView result = orderQueryService.getOrderListWithCondition(query);

                assertThat(result.getItems())
                        .hasSizeGreaterThanOrEqualTo(2)
                        .isSortedAccordingTo((item1, item2) ->
                                item1.getCreatedAt().value().compareTo(item2.getCreatedAt().value())
                        );
            }

            @Test
            @DisplayName("생성일시 내림차순으로 정렬된다.")
            void 생성일시_내림차순으로_정렬된다() {
                GetOrderListQuery query = new GetOrderListQuery(
                        savedUserIdentifier,
                        "DESC",
                        0,
                        10
                );

                OrderListView result = orderQueryService.getOrderListWithCondition(query);

                assertThat(result.getItems())
                        .hasSizeGreaterThanOrEqualTo(2)
                        .isSortedAccordingTo((item1, item2) ->
                                item2.getCreatedAt().value().compareTo(item1.getCreatedAt().value())
                        );
            }

            @Test
            @DisplayName("특정 사용자의 주문만 조회된다.")
            void 특정_사용자의_주문만_조회된다() {
                orderRepository.save(Order.create(otherUserId));
                orderRepository.save(Order.create(otherUserId));

                GetOrderListQuery query = new GetOrderListQuery(
                        savedUserIdentifier,
                        "ASC",
                        0,
                        10
                );

                OrderListView result = orderQueryService.getOrderListWithCondition(query);

                assertSoftly(softly -> {
                    softly.assertThat(result.getTotalElements()).isEqualTo(3);
                    softly.assertThat(result.getItems())
                            .allMatch(item -> item.getUserId().value().equals(savedUserId.value()),
                                    "모든 주문이 특정 사용자에 속해야 함");
                });
            }
        }

        @Nested
        @DisplayName("조건에 맞는 주문이 없는 경우")
        class 조건에_맞는_주문이_없는_경우 {

            @Test
            @DisplayName("빈 리스트가 반환된다.")
            void 빈_리스트가_반환된다() {
                GetOrderListQuery query = new GetOrderListQuery(
                        savedUserIdentifier,
                        "ASC",
                        0,
                        10
                );

                OrderListView result = orderQueryService.getOrderListWithCondition(query);

                assertSoftly(softly -> {
                    softly.assertThat(result.getItems()).isEmpty();
                    softly.assertThat(result.getTotalElements()).isZero();
                    softly.assertThat(result.getTotalPages()).isZero();
                    softly.assertThat(result.isHasNext()).isFalse();
                    softly.assertThat(result.isHasPrevious()).isFalse();
                });
            }
        }

        @Nested
        @DisplayName("존재하지 않는 사용자로 조회할 때")
        class 존재하지_않는_사용자로_조회할_때 {

            @Test
            @DisplayName("NotFoundException이 던져진다.")
            void NotFoundException이_던져진다() {
                GetOrderListQuery query = new GetOrderListQuery(
                        "nonexistent",
                        "ASC",
                        0,
                        10
                );

                assertThatThrownBy(() -> orderQueryService.getOrderListWithCondition(query))
                        .isInstanceOf(NotFoundException.class)
                        .hasMessageContaining("사용자");
            }
        }
    }

    @Nested
    @DisplayName("주문 상세 조회 시")
    class 주문_상세_조회_시 {

        private BrandId savedBrandId;
        private UserId savedUserId;
        private String savedOrderId;

        @BeforeEach
        void setUp() {
            Brand brand = brandRepository.save(Brand.create(
                    new BrandName("loopers"),
                    new BrandDescription("education brand")
            ));
            savedBrandId = brand.getId();

            User user = userRepository.save(User.create(
                    UserIdentifier.create("loopers"),
                    new UserEmail("loopers@test.com"),
                    UserBirthDay.create("1990-01-01"),
                    UserGender.create("MALE")
            ));
            savedUserId = user.getUserId();

            User otherUser = userRepository.save(User.create(
                    UserIdentifier.create("other"),
                    new UserEmail("other@test.com"),
                    UserBirthDay.create("1991-02-02"),
                    UserGender.create("FEMALE")
            ));

            Order order = orderRepository.save(Order.create(savedUserId));
            savedOrderId = order.getOrderId().value();

            Order otherOrder = orderRepository.save(Order.create(otherUser.getUserId()));
        }

        @Nested
        @DisplayName("주문이 존재하는 경우")
        class 주문이_존재하는_경우 {

            @BeforeEach
            void setUp() {
                Product product1 = productRepository.save(
                        Product.create(
                                savedBrandId,
                                new ProductName("MacBook Pro"),
                                new ProductPrice(new BigDecimal(1_300_000)),
                                new ProductStock(100L)
                        )
                );

                Product product2 = productRepository.save(
                        Product.create(
                                savedBrandId,
                                new ProductName("iPad Air"),
                                new ProductPrice(new BigDecimal(800_000)),
                                new ProductStock(100L)
                        )
                );

                OrderId orderId = new OrderId(savedOrderId);
                orderItemRepository.save(OrderItem.create(
                        orderId,
                        product1.getId(),
                        new Quantity(1L)
                ));

                orderItemRepository.save(OrderItem.create(
                        orderId,
                        product2.getId(),
                        new Quantity(2L)
                ));
            }

            @Test
            @DisplayName("주문 상세가 조회된다.")
            void 주문_상세가_조회된다() {
                GetOrderDetailQuery query = new GetOrderDetailQuery(savedOrderId);

                OrderDetail result = orderQueryService.getOrderDetail(query);

                assertSoftly(softly -> {
                    softly.assertThat(result).isNotNull();
                    softly.assertThat(result.getOrder()).isNotNull();
                    softly.assertThat(result.getOrder().getUserId().value()).isEqualTo(savedUserId.value());
                });
            }

            @Test
            @DisplayName("주문 아이템이 모두 조회된다.")
            void 주문_아이템이_모두_조회된다() {
                GetOrderDetailQuery query = new GetOrderDetailQuery(savedOrderId);

                OrderDetail result = orderQueryService.getOrderDetail(query);

                assertSoftly(softly -> {
                    softly.assertThat(result.getOrderItems()).isNotEmpty();
                    softly.assertThat(result.getOrderItems()).hasSize(2);
                });
            }

            @Test
            @DisplayName("주문 아이템의 순서 ID가 일치한다.")
            void 주문_아이템의_주문_ID가_일치한다() {
                GetOrderDetailQuery query = new GetOrderDetailQuery(savedOrderId);

                OrderDetail result = orderQueryService.getOrderDetail(query);

                assertThat(result.getOrderItems())
                        .allMatch(item -> item.getOrderId().value().equals(savedOrderId),
                                "모든 주문 아이템이 동일한 주문에 속해야 함");
            }
        }

        @Nested
        @DisplayName("주문이 존재하지 않는 경우")
        class 주문이_존재하지_않는_경우 {

            @Test
            @DisplayName("NotFoundException이 던져진다.")
            void NotFoundException이_던져진다() {
                GetOrderDetailQuery query = new GetOrderDetailQuery("99999");

                assertThatThrownBy(() -> orderQueryService.getOrderDetail(query))
                        .isInstanceOf(NotFoundException.class)
                        .hasMessageContaining("주문");
            }
        }

        @Nested
        @DisplayName("주문 아이템이 없는 경우")
        class 주문_아이템이_없는_경우 {

            @Test
            @DisplayName("빈 주문 아이템 리스트가 반환된다.")
            void 빈_주문_아이템_리스트가_반환된다() {
                GetOrderDetailQuery query = new GetOrderDetailQuery(savedOrderId);

                OrderDetail result = orderQueryService.getOrderDetail(query);

                assertSoftly(softly -> {
                    softly.assertThat(result.getOrder()).isNotNull();
                    softly.assertThat(result.getOrderItems()).isEmpty();
                });
            }
        }
    }
}
