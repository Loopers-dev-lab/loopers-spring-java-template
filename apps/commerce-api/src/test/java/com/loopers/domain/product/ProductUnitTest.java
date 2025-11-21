package com.loopers.domain.product;

import java.lang.reflect.Field;
import java.math.BigDecimal;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.util.ReflectionUtils;

import com.loopers.domain.brand.BrandEntity;
import com.loopers.fixtures.BrandTestFixture;

/**
 * @author hyunjikoh
 * @since 2025. 11. 10.
 */
@DisplayName("ProductEntity 단위 테스트")
class ProductUnitTest {

    @Nested
    @DisplayName("상품 엔티티 생성")
    class 상품_엔티티_생성 {

        @Test
        @DisplayName("유효한 정보로 상품 엔티티를 생성하면 성공한다")
        void 유효한_정보로_상품_엔티티를_생성하면_성공한다() {
            // given
            BrandEntity brand = BrandTestFixture.createEntity("Nike", "Just Do It");
            ProductDomainCreateRequest productDomainRequest = ProductDomainCreateRequest.of(
                    brand.getId(), "Air Max", "Comfortable running shoes", new BigDecimal("100000"), 50
            );

            // when
            ProductEntity entity = ProductEntity.createEntity(productDomainRequest);

            // then
            Assertions.assertThat(entity).isNotNull();
            Assertions.assertThat(entity.getBrandId()).isEqualTo(brand.getId());
            Assertions.assertThat(entity.getName()).isEqualTo("Air Max");
        }

        @Test
        @DisplayName("할인가가 있는 상품 엔티티를 생성하면 성공한다")
        void 할인가가_있는_상품_엔티티를_생성하면_성공한다() {
            // given
            BrandEntity brand = BrandTestFixture.createEntity("Nike", "Just Do It");
            ProductDomainCreateRequest productDomainRequest = new ProductDomainCreateRequest(
                    brand.getId(), "Air Max", "Comfortable running shoes", new BigDecimal("100000.00"), new BigDecimal("80000.00"), 50
            );

            // when
            ProductEntity entity = ProductEntity.createEntity(productDomainRequest);

            // then
            Assertions.assertThat(entity).isNotNull();
            Assertions.assertThat(entity.getBrandId()).isEqualTo(brand.getId());
            Assertions.assertThat(entity.getName()).isEqualTo(productDomainRequest.name());
            Assertions.assertThat(entity.getPrice().getOriginPrice()).isEqualTo(productDomainRequest.originPrice());
            Assertions.assertThat(entity.getPrice().getDiscountPrice()).isEqualTo(productDomainRequest.discountPrice());

        }

        @Test
        @DisplayName("브랜드 ID가 null인 경우 예외가 발생한다")
        void 브랜드가_null인_경우_예외가_발생한다() {
            ProductDomainCreateRequest productDomainRequest = new ProductDomainCreateRequest(
                    null, "Air Max", "Comfortable running shoes", new BigDecimal("100000.00"), new BigDecimal("100000.00"), 50
            );

            Assertions.assertThatThrownBy(() -> ProductEntity.createEntity(productDomainRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("브랜드 ID는 필수입니다.");
        }

        @Test
        @DisplayName("상품명이 null인 경우 예외가 발생한다")
        void 상품명이_null인_경우_예외가_발생한다() {
            BrandEntity brand = BrandTestFixture.createEntity("Nike", "Just Do It");

            ProductDomainCreateRequest productDomainRequest = new ProductDomainCreateRequest(
                    brand.getId(), null, "Comfortable running shoes", new BigDecimal("100000.00"), new BigDecimal("100000.00"), 50
            );
            Assertions.assertThatThrownBy(() -> ProductEntity.createEntity(productDomainRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("상품명은 필수입니다.");

        }

        @Test
        @DisplayName("상품명이 200자를 초과하는 경우 예외가 발생한다")
        void 상품명이_200자를_초과하는_경우_예외가_발생한다() {
            // given
            BrandEntity brand = BrandTestFixture.createEntity("Nike", "Just Do It");
            String tooLongName = "a".repeat(201);

            // when & then
            Assertions.assertThatThrownBy(() -> {
                        ProductDomainCreateRequest request = new ProductDomainCreateRequest(
                                brand.getId(), tooLongName, "description",
                                new BigDecimal("10000"), null, 100
                        );
                        ProductEntity.createEntity(request);
                    }).isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("상품명은 200자를 초과할 수 없습니다.");
        }

        @Test
        @DisplayName("재고 수량이 음수인 경우 예외가 발생한다")
        void 재고_수량이_음수인_경우_예외가_발생한다() {
            // given
            BrandEntity brand = BrandTestFixture.createEntity("Nike", "Just Do It");

            // when & then
            Assertions.assertThatThrownBy(() -> {
                        ProductDomainCreateRequest request = new ProductDomainCreateRequest(
                                brand.getId(), "상품명", "description",
                                new BigDecimal("10000"), null, -1
                        );
                        ProductEntity.createEntity(request);
                    }).isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("재고 수량은 0 이상이어야 합니다.");
        }
    }

    @Nested
    @DisplayName("좋아요 관리")
    class 좋아요_관리 {
        private ProductEntity product;
        private BrandEntity brand;

        @BeforeEach
        void setUp() {
            brand = BrandTestFixture.createEntity("Nike", "Just Do It");
            product = ProductEntity.createEntity(
                    ProductDomainCreateRequest.of(
                            brand.getId(), "Test Product", "Description",
                            new BigDecimal("10000"), 100
                    )
            );
        }

        @Test
        @DisplayName("좋아요 수를 증가시키면 성공한다")
        void 좋아요_수를_증가시키면_성공한다() {
            // given
            long initialLikes = product.getLikeCount();

            // when
            product.increaseLikeCount();

            // then
            Assertions.assertThat(product.getLikeCount())
                    .isEqualTo(initialLikes + 1);
        }

        @Test
        @DisplayName("좋아요 수가 0보다 클 때 감소시키면 성공한다")
        void 좋아요_수가_0보다_클_때_감소시키면_성공한다() {
            // given
            product.increaseLikeCount(); // 좋아요 1 증가
            long initialLikes = product.getLikeCount();

            // when
            product.decreaseLikeCount();

            // then
            Assertions.assertThat(product.getLikeCount())
                    .isEqualTo(initialLikes - 1);
        }

        @Test
        @DisplayName("좋아요 수가 0일 때 감소시켜도 0을 유지한다")
        void 좋아요_수가_0일_때_감소시켜도_0을_유지한다() {
            // given
            Assertions.assertThat(product.getLikeCount()).isZero(); // 초기값 0 확인

            // when
            product.decreaseLikeCount();

            // then
            Assertions.assertThat(product.getLikeCount()).isZero();
        }
    }

    @Nested
    @DisplayName("재고 여부 확인")
    class 재고_여부_확인 {
        private ProductEntity product;
        private BrandEntity brand;

        @BeforeEach
        void setUp() {
            brand = BrandTestFixture.createEntity("Nike", "Just Do It");
        }

        @Test
        @DisplayName("재고가 있으면 true를 반환한다")
        void 재고가_있으면_true를_반환한다() {
            // given
            product = ProductEntity.createEntity(
                    ProductDomainCreateRequest.of(
                            brand.getId(), "Test Product", "Description",
                            new BigDecimal("10000"), 100
                    )
            );

            // when & then
            Assertions.assertThat(product.hasStock()).isTrue();
        }

        @Test
        @DisplayName("재고가 0이면 false를 반환한다")
        void 재고가_0이면_false를_반환한다() {
            // given
            product = ProductEntity.createEntity(
                    ProductDomainCreateRequest.of(
                            brand.getId(), "Test Product", "Description",
                            new BigDecimal("10000"), 0
                    )
            );

            // when & then
            Assertions.assertThat(product.hasStock()).isFalse();
        }
    }

    @Nested
    @DisplayName("주문 가능 여부 확인")
    class 주문_가능_여부_확인 {
        private ProductEntity product;
        private BrandEntity brand;

        @BeforeEach
        void setUp() {
            brand = BrandTestFixture.createEntity("Nike", "Just Do It");
            product = ProductEntity.createEntity(
                    ProductDomainCreateRequest.of(
                            brand.getId(), "Test Product", "Description",
                            new BigDecimal("10000"), 100
                    )
            );
        }

        @Test
        @DisplayName("요청 수량이 재고 내에 있으면 true를 반환한다")
        void 요청_수량이_재고_내에_있으면_true를_반환한다() {
            // given
            int orderQuantity = product.getStockQuantity() - 1;

            // when & then
            Assertions.assertThat(product.canOrder(orderQuantity)).isTrue();
        }

        @Test
        @DisplayName("요청 수량이 재고와 정확히 같으면 true를 반환한다")
        void 요청_수량이_재고와_정확히_같으면_true를_반환한다() {
            // given
            int orderQuantity = product.getStockQuantity();

            // when & then
            Assertions.assertThat(product.canOrder(orderQuantity)).isTrue();
        }

        @Test
        @DisplayName("요청 수량이 재고보다 많으면 false를 반환한다")
        void 요청_수량이_재고보다_많으면_false를_반환한다() {
            // given
            int orderQuantity = product.getStockQuantity() + 1;

            // when & then
            Assertions.assertThat(product.canOrder(orderQuantity)).isFalse();
        }

        @Test
        @DisplayName("요청 수량이 0 이하이면 false를 반환한다")
        void 요청_수량이_0_이하이면_false를_반환한다() {
            // when & then
            Assertions.assertThat(product.canOrder(0)).isFalse();
            Assertions.assertThat(product.canOrder(-1)).isFalse();
        }

        @Test
        @DisplayName("재고가 없으면 false를 반환한다")
        void 재고가_없으면_false를_반환한다() {
            // given
            product.setStockQuantity(0);

            // when & then
            Assertions.assertThat(product.canOrder(1)).isFalse();
        }
    }

    @Nested
    @DisplayName("판매 가격 조회")
    class 판매_가격_조회 {
        private ProductEntity product;
        private BrandEntity brand;
        private final BigDecimal ORIGIN_PRICE = new BigDecimal("10000");
        private final BigDecimal DISCOUNT_PRICE = new BigDecimal("8000");

        @BeforeEach
        void setUp() {
            brand = BrandTestFixture.createEntity("Nike", "Just Do It");
        }

        @Test
        @DisplayName("할인이 없는 경우 정가를 반환한다")
        void 할인이_없는_경우_정가를_반환한다() {
            // given
            product = ProductEntity.createEntity(
                    ProductDomainCreateRequest.of(
                            brand.getId(), "Test Product", "Description",
                            ORIGIN_PRICE, 100
                    )
            );

            // when & then
            Assertions.assertThat(product.getSellingPrice())
                    .isEqualByComparingTo(ORIGIN_PRICE);
        }

        @Test
        @DisplayName("할인이 있는 경우 할인가를 반환한다")
        void 할인이_있는_경우_할인가를_반환한다() {
            // given
            product = ProductEntity.createEntity(
                    new ProductDomainCreateRequest(
                            brand.getId(), "Test Product", "Description",
                            ORIGIN_PRICE, DISCOUNT_PRICE, 100
                    )
            );

            // when & then
            Assertions.assertThat(product.getSellingPrice())
                    .isEqualByComparingTo(DISCOUNT_PRICE);
        }
    }

    @Nested
    @DisplayName("할인 여부 확인")
    class 할인_여부_확인 {
        private ProductEntity product;
        private BrandEntity brand;
        private final BigDecimal ORIGIN_PRICE = new BigDecimal("10000");
        private final BigDecimal DISCOUNT_PRICE = new BigDecimal("8000");

        @BeforeEach
        void setUp() {
            brand = BrandTestFixture.createEntity("Nike", "Just Do It");
        }

        @Test
        @DisplayName("할인이 적용된 상품이면 true를 반환한다")
        void 할인이_적용된_상품이면_true를_반환한다() {
            // given
            product = ProductEntity.createEntity(
                    new ProductDomainCreateRequest(
                            brand.getId(), "Test Product", "Description",
                            ORIGIN_PRICE, DISCOUNT_PRICE, 100
                    )
            );

            // when & then
            Assertions.assertThat(product.isDiscounted()).isTrue();
        }

        @Test
        @DisplayName("할인이 없는 상품이면 false를 반환한다")
        void 할인이_없는_상품이면_false를_반환한다() {
            // given
            product = ProductEntity.createEntity(
                    ProductDomainCreateRequest.of(
                            brand.getId(), "Test Product", "Description",
                            ORIGIN_PRICE, 100
                    )
            );

            // when & then
            Assertions.assertThat(product.isDiscounted()).isFalse();
        }
    }

    @Nested
    @DisplayName("재고 차감")
    class 재고_차감 {
        private ProductEntity product;
        private BrandEntity brand;

        @BeforeEach
        void setUp() {
            brand = BrandTestFixture.createEntity("Nike", "Just Do It");
            product = ProductEntity.createEntity(
                    ProductDomainCreateRequest.of(
                            brand.getId(), "Test Product", "Description",
                            new BigDecimal("10000"), 100
                    )
            );
        }

        @Test
        @DisplayName("정상적인 재고 차감이 성공한다")
        void deduct_stock_success() {
            // given
            int deductQuantity = 10;
            int initialStock = product.getStockQuantity();

            // when
            product.deductStock(deductQuantity);

            // then
            Assertions.assertThat(product.getStockQuantity())
                    .isEqualTo(initialStock - deductQuantity);
        }

        @Test
        @DisplayName("재고보다 많은 수량을 차감하면 CoreException이 발생한다")
        void deduct_stock_insufficient() {
            // given
            int deductQuantity = product.getStockQuantity() + 1;

            // when & then
            Assertions.assertThatThrownBy(() -> product.deductStock(deductQuantity))
                    .isInstanceOf(com.loopers.support.error.CoreException.class)
                    .hasMessageContaining("재고가 부족합니다");
        }

        @Test
        @DisplayName("0 이하의 수량을 차감하면 IllegalArgumentException이 발생한다")
        void deduct_stock_invalid_quantity() {
            // when & then
            Assertions.assertThatThrownBy(() -> product.deductStock(0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("차감할 재고 수량은 0보다 커야 합니다.");

            Assertions.assertThatThrownBy(() -> product.deductStock(-1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("차감할 재고 수량은 0보다 커야 합니다.");
        }
    }

    @Nested
    @DisplayName("엔티티 검증")
    class 엔티티_검증 {
        private BrandEntity brand;
        private final BigDecimal ORIGIN_PRICE = new BigDecimal("10000");
        private final int STOCK_QUANTITY = 100;

        @BeforeEach
        void setUp() {
            brand = BrandTestFixture.createEntity("Nike", "Just Do It");
        }

        @Test
        @DisplayName("모든 필수 값이 유효하면 검증에 성공한다")
        void 모든_필수_값이_유효하면_검증에_성공한다() {
            // given
            ProductEntity product = ProductEntity.createEntity(
                    ProductDomainCreateRequest.of(
                            brand.getId(), "Test Product", "Description",
                            ORIGIN_PRICE, STOCK_QUANTITY
                    )
            );

            // when & then
            Assertions.assertThatCode(() -> product.guard())
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("브랜드 ID가 null이면 검증에 실패한다")
        void 브랜드_ID가_null이면_검증에_실패한다() {
            // given
            ProductEntity product = ProductEntity.createEntity(
                    ProductDomainCreateRequest.of(
                            brand.getId(), "Test Product", "Description",
                            ORIGIN_PRICE, STOCK_QUANTITY
                    )
            );

            // when
            Field brandIdField = ReflectionUtils.findField(ProductEntity.class, "brandId");
            ReflectionUtils.makeAccessible(brandIdField);
            ReflectionUtils.setField(brandIdField, product, null);

            // then
            Assertions.assertThatThrownBy(() -> product.guard())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("브랜드 ID는 필수입니다.");
        }

        @Test
        @DisplayName("가격 정보가 null이면 검증에 실패한다")
        void 가격_정보가_null이면_검증에_실패한다() {
            // given
            ProductEntity product = ProductEntity.createEntity(
                    ProductDomainCreateRequest.of(
                            brand.getId(), "Test Product", "Description",
                            ORIGIN_PRICE, STOCK_QUANTITY
                    )
            );

            // when
            Field priceField = ReflectionUtils.findField(ProductEntity.class, "price");
            ReflectionUtils.makeAccessible(priceField);
            ReflectionUtils.setField(priceField, product, null);

            // then
            Assertions.assertThatThrownBy(() -> product.guard())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("가격 정보는 필수입니다.");
        }

        @Test
        @DisplayName("상품명이 유효하지 않으면 검증에 실패한다")
        void 상품명이_유효하지_않으면_검증에_실패한다() {
            // given
            ProductEntity product = ProductEntity.createEntity(
                    ProductDomainCreateRequest.of(
                            brand.getId(), "Test Product", "Description",
                            ORIGIN_PRICE, STOCK_QUANTITY
                    )
            );

            // when
            Field nameField = ReflectionUtils.findField(ProductEntity.class, "name");
            ReflectionUtils.makeAccessible(nameField);

            // 상품명이 null인 경우
            ReflectionUtils.setField(nameField, product, null);

            // then
            Assertions.assertThatThrownBy(() -> product.guard())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("상품명은 비어있을 수 없습니다.");

            // 상품명이 빈 문자열인 경우
            ReflectionUtils.setField(nameField, product, "");

            Assertions.assertThatThrownBy(() -> product.guard())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("상품명은 비어있을 수 없습니다.");

            // 상품명이 200자 초과인 경우
            ReflectionUtils.setField(nameField, product, "a".repeat(201));

            Assertions.assertThatThrownBy(() -> product.guard())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("상품명은 200자를 초과할 수 없습니다.");
        }

        @Test
        @DisplayName("재고 수량이 음수이면 검증에 실패한다")
        void 재고_수량이_음수이면_검증에_실패한다() {
            // given
            ProductEntity product = ProductEntity.createEntity(
                    ProductDomainCreateRequest.of(
                            brand.getId(), "Test Product", "Description",
                            ORIGIN_PRICE, STOCK_QUANTITY
                    )
            );

            // when
            Field stockQuantityField = ReflectionUtils.findField(ProductEntity.class, "stockQuantity");
            ReflectionUtils.makeAccessible(stockQuantityField);
            ReflectionUtils.setField(stockQuantityField, product, -1);

            // then
            Assertions.assertThatThrownBy(() -> product.guard())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("재고 수량은 0 이상이어야 합니다.");
        }

        @Test
        @DisplayName("좋아요 수가 음수이면 검증에 실패한다")
        void 좋아요_수가_음수이면_검증에_실패한다() {
            // given
            ProductEntity product = ProductEntity.createEntity(
                    ProductDomainCreateRequest.of(
                            brand.getId(), "Test Product", "Description",
                            ORIGIN_PRICE, STOCK_QUANTITY
                    )
            );

            // when
            Field likeCountField = ReflectionUtils.findField(ProductEntity.class, "likeCount");
            ReflectionUtils.makeAccessible(likeCountField);
            ReflectionUtils.setField(likeCountField, product, -1L);

            // then
            Assertions.assertThatThrownBy(() -> product.guard())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("좋아요 수는 0 이상이어야 합니다.");
        }
    }
}
