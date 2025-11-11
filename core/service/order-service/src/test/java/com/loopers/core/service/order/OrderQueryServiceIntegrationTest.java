package com.loopers.core.service.order;

import com.loopers.core.domain.error.NotFoundException;
import com.loopers.core.domain.order.Order;
import com.loopers.core.domain.order.OrderListView;
import com.loopers.core.domain.order.repository.OrderRepository;
import com.loopers.core.domain.user.User;
import com.loopers.core.domain.user.repository.UserRepository;
import com.loopers.core.domain.user.type.UserGender;
import com.loopers.core.domain.user.vo.UserBirthDay;
import com.loopers.core.domain.user.vo.UserEmail;
import com.loopers.core.domain.user.vo.UserId;
import com.loopers.core.domain.user.vo.UserIdentifier;
import com.loopers.core.service.IntegrationTest;
import com.loopers.core.service.order.query.GetOrderListQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class OrderQueryServiceIntegrationTest extends IntegrationTest {

    @Autowired
    private OrderQueryService orderQueryService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

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
}
