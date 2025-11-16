package com.loopers.core.service.order;

import com.loopers.core.domain.brand.vo.BrandId;
import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.DeletedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.order.OrderItem;
import com.loopers.core.domain.order.vo.OrderId;
import com.loopers.core.domain.order.vo.Quantity;
import com.loopers.core.domain.product.Product;
import com.loopers.core.domain.product.repository.ProductRepository;
import com.loopers.core.domain.product.vo.*;
import com.loopers.core.service.ConcurrencyTestUtil;
import com.loopers.core.service.IntegrationTest;
import com.loopers.core.service.order.component.OrderLineAllocator;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

@DisplayName("주문 라인 할당")
class OrderLineAllocatorIntegrationTest extends IntegrationTest {

    @Autowired
    private OrderLineAllocator orderLineAllocator;

    @Autowired
    private ProductRepository productRepository;

    @Nested
    @DisplayName("상품 할당 시")
    class 상품_할당 {

        @Nested
        @DisplayName("충분한 재고가 있는 경우")
        class 충분한_재고가_있는_경우 {

            private Product product;
            private OrderItem orderItem;

            @BeforeEach
            void setUp() {
                // 상품 생성 및 저장 (가격: 10,000, 재고: 100개)
                product = Product.mappedBy(
                        ProductId.empty(),
                        new BrandId("1"),
                        new ProductName("테스트상품"),
                        new ProductPrice(new BigDecimal("10000")),
                        new ProductStock(100L),
                        ProductLikeCount.init(),
                        CreatedAt.now(),
                        UpdatedAt.now(),
                        DeletedAt.empty()
                );
                product = productRepository.save(product);

                // 주문 상품 생성 (수량: 5개)
                orderItem = OrderItem.create(
                        new OrderId("1"),
                        product.getId(),
                        new Quantity(5L)
                );
            }

            @Test
            @DisplayName("상품의 재고를 차감하고 총 가격을 반환한다")
            void 상품의_재고를_차감하고_총_가격을_반환한다() {
                // when
                BigDecimal totalPrice = orderLineAllocator.allocate(orderItem);

                // then - 반환된 가격 검증 (10,000 * 5 = 50,000)
                SoftAssertions.assertSoftly(softly -> {
                    softly.assertThat(totalPrice)
                            .as("할당된 총 가격")
                            .isEqualByComparingTo(new BigDecimal("50000"));
                });

                // 상품 재고 차감 확인
                Product allocatedProduct = productRepository.getById(product.getId());
                SoftAssertions.assertSoftly(softly -> {
                    softly.assertThat(allocatedProduct.getStock().value())
                            .as("차감 후 남은 재고")
                            .isEqualTo(95L); // 100 - 5
                });
            }

            @Test
            @DisplayName("여러 번 할당하면 재고가 누적으로 차감된다")
            void 여러_번_할당하면_재고가_누적으로_차감된다() {
                // when - 첫 번째 할당 (5개)
                orderLineAllocator.allocate(orderItem);

                // 두 번째 할당 (5개)
                orderLineAllocator.allocate(orderItem);

                // then - 재고 누적 차감 확인
                Product allocatedProduct = productRepository.getById(product.getId());
                SoftAssertions.assertSoftly(softly -> {
                    softly.assertThat(allocatedProduct.getStock().value())
                            .as("누적 차감 후 남은 재고")
                            .isEqualTo(90L); // 100 - 5 - 5
                });
            }

            @Test
            @DisplayName("정확히 남은 재고만큼 할당하면 재고가 0이 된다")
            void 정확히_남은_재고만큼_할당하면_재고가_0이_된다() {
                // given - 정확히 현재 재고만큼 주문
                OrderItem exactOrderItem = OrderItem.create(
                        new OrderId("1"),
                        product.getId(),
                        new Quantity(100L)
                );

                // when
                BigDecimal totalPrice = orderLineAllocator.allocate(exactOrderItem);

                // then - 총 가격 검증 (10,000 * 100 = 1,000,000)
                SoftAssertions.assertSoftly(softly -> {
                    softly.assertThat(totalPrice)
                            .as("할당된 총 가격")
                            .isEqualByComparingTo(new BigDecimal("1000000"));
                });

                // 재고가 0이 되었는지 확인
                Product allocatedProduct = productRepository.getById(product.getId());
                SoftAssertions.assertSoftly(softly -> {
                    softly.assertThat(allocatedProduct.getStock().value())
                            .as("차감 후 남은 재고")
                            .isZero();
                });
            }

            @Test
            @DisplayName("동시에 여러 사용자가 주문하면 주문한양만큼 감소한다.")
            void 동시에_여러_사용자가_주문하면_주문한양만큼_감소한다() throws InterruptedException {
                int requestCount = 100;
                OrderItem orderItem = OrderItem.create(
                        new OrderId("1"),
                        product.getId(),
                        new Quantity(1L)
                );

                List<BigDecimal> results = ConcurrencyTestUtil.executeInParallel(
                        100,
                        index -> orderLineAllocator.allocate(orderItem)
                );

                Product actualProduct = productRepository.getById(product.getId());


                assertSoftly(softly -> {
                    softly.assertThat(results).as("동시 요청 결과 수").hasSize(requestCount);
                    softly.assertThat(actualProduct.getStock().value()).as("최종 재고 수").isEqualTo(0L);
                });
            }
        }

        @Nested
        @DisplayName("재고가 부족한 경우")
        class 재고가_부족한_경우 {

            private Product product;

            @BeforeEach
            void setUp() {
                // 상품 생성 및 저장 (가격: 10,000, 재고: 100개)
                product = Product.mappedBy(
                        ProductId.empty(),
                        new BrandId("1"),
                        new ProductName("테스트상품"),
                        new ProductPrice(new BigDecimal("10000")),
                        new ProductStock(100L),
                        ProductLikeCount.init(),
                        CreatedAt.now(),
                        UpdatedAt.now(),
                        DeletedAt.empty()
                );
                product = productRepository.save(product);
            }

            @Test
            @DisplayName("예외를 발생시킨다")
            void 예외를_발생시킨다() {
                // given - 재고보다 많은 수량 주문
                OrderItem largeOrderItem = OrderItem.create(
                        new OrderId("1"),
                        product.getId(),
                        new Quantity(150L)
                );

                // when & then
                org.assertj.core.api.Assertions.assertThatThrownBy(
                                () -> orderLineAllocator.allocate(largeOrderItem)
                        ).isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("상품의 재고가 부족합니다.");
            }
        }
    }
}
