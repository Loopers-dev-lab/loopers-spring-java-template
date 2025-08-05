package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class ProductOptionModelTest {

    @Nested
    @DisplayName("상품 옵션 생성 관련 테스트")
    class CreateTest {

        @DisplayName("정상적인 값으로 상품 옵션을 생성할 수 있다")
        @Test
        void create_withValidValues() {
            // arrange
            Long productId = 1L;
            String name = "색상";
            String value = "빨간색";
            BigDecimal additionalPrice = new BigDecimal(5000);

            // act
            ProductOptionModel option = ProductOptionModel.create(productId, name, value, additionalPrice);

            // assert
            assertAll(
                    () -> assertThat(option).isNotNull(),
                    () -> assertThat(option.getProductId().getValue()).isEqualTo(productId),
                    () -> assertThat(option.getName().getValue()).isEqualTo(name),
                    () -> assertThat(option.getValue().getValue()).isEqualTo(value),
                    () -> assertThat(option.getAdditionalPrice().getValue()).isEqualTo(additionalPrice)
            );
        }

        @DisplayName("상품 ID가 null이면 생성에 실패한다")
        @Test
        void create_whenProductIdNull() {
            // arrange
            Long productId = null;
            String name = "색상";
            String value = "빨간색";
            BigDecimal additionalPrice = new BigDecimal(5000);

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                ProductOptionModel.create(productId, name, value, additionalPrice);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("productId는 양수여야 합니다");
        }

        @DisplayName("상품 ID가 0 이하면 생성에 실패한다")
        @Test
        void create_whenProductIdZeroOrNegative() {
            // arrange
            String name = "색상";
            String value = "빨간색";
            BigDecimal additionalPrice = new BigDecimal(5000);

            // act & assert
            assertAll(
                    () -> {
                        CoreException exception = assertThrows(CoreException.class, () -> {
                            ProductOptionModel.create(0L, name, value, additionalPrice);
                        });
                        assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
                    },
                    () -> {
                        CoreException exception = assertThrows(CoreException.class, () -> {
                            ProductOptionModel.create(-1L, name, value, additionalPrice);
                        });
                        assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
                    }
            );
        }

        @DisplayName("옵션명이 null이거나 빈 문자열이면 생성에 실패한다")
        @Test
        void create_whenOptionNameNullOrEmpty() {
            // arrange
            Long productId = 1L;
            String value = "빨간색";
            BigDecimal additionalPrice = new BigDecimal(5000);

            // act & assert
            assertAll(
                    () -> {
                        CoreException exception = assertThrows(CoreException.class, () -> {
                            ProductOptionModel.create(productId, null, value, additionalPrice);
                        });
                        assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
                    },
                    () -> {
                        CoreException exception = assertThrows(CoreException.class, () -> {
                            ProductOptionModel.create(productId, "", value, additionalPrice);
                        });
                        assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
                    },
                    () -> {
                        CoreException exception = assertThrows(CoreException.class, () -> {
                            ProductOptionModel.create(productId, "   ", value, additionalPrice);
                        });
                        assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
                    }
            );
        }

        @DisplayName("옵션값이 null이거나 빈 문자열이면 생성에 실패한다")
        @Test
        void create_whenOptionValueNullOrEmpty() {
            // arrange
            Long productId = 1L;
            String name = "색상";
            BigDecimal additionalPrice = new BigDecimal(5000);

            // act & assert
            assertAll(
                    () -> {
                        CoreException exception = assertThrows(CoreException.class, () -> {
                            ProductOptionModel.create(productId, name, null, additionalPrice);
                        });
                        assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
                    },
                    () -> {
                        CoreException exception = assertThrows(CoreException.class, () -> {
                            ProductOptionModel.create(productId, name, "", additionalPrice);
                        });
                        assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
                    },
                    () -> {
                        CoreException exception = assertThrows(CoreException.class, () -> {
                            ProductOptionModel.create(productId, name, "   ", additionalPrice);
                        });
                        assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
                    }
            );
        }

        @DisplayName("추가 가격이 null이면 생성에 실패한다")
        @Test
        void create_whenAdditionalPriceNull() {
            // arrange
            Long productId = 1L;
            String name = "색상";
            String value = "빨간색";
            BigDecimal additionalPrice = null;

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                ProductOptionModel.create(productId, name, value, additionalPrice);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("추가 가격은 null일 수 없습니다");
        }
    }

    @Nested
    @DisplayName("총 가격 계산 관련 테스트")
    class CalculateTotalPriceTest {

        @DisplayName("기본 가격에 추가 가격을 더해 총 가격을 계산할 수 있다")
        @Test
        void calculateTotalPrice_withPositiveAdditionalPrice() {
            // arrange
            ProductOptionModel option = ProductOptionFixture.createWithAdditionalPrice(new BigDecimal(5000));
            BigDecimal basePrice = new BigDecimal(10000);

            // act
            BigDecimal totalPrice = option.calculateTotalPrice(basePrice);

            // assert
            assertThat(totalPrice).isEqualByComparingTo(new BigDecimal(15000));
        }

        @DisplayName("추가 가격이 0인 경우 기본 가격과 동일하다")
        @Test
        void calculateTotalPrice_withZeroAdditionalPrice() {
            // arrange
            ProductOptionModel option = ProductOptionFixture.createFreeOption();
            BigDecimal basePrice = new BigDecimal(10000);

            // act
            BigDecimal totalPrice = option.calculateTotalPrice(basePrice);

            // assert
            assertThat(totalPrice).isEqualByComparingTo(new BigDecimal(10000));
        }

        @DisplayName("추가 가격이 음수인 경우 할인이 적용된다")
        @Test
        void calculateTotalPrice_withNegativeAdditionalPrice() {
            // arrange
            ProductOptionModel option = ProductOptionFixture.createDiscountOption();
            BigDecimal basePrice = new BigDecimal(10000);

            // act
            BigDecimal totalPrice = option.calculateTotalPrice(basePrice);

            // assert
            assertThat(totalPrice).isEqualByComparingTo(new BigDecimal(9000)); // 10000 - 1000
        }

        @DisplayName("기본 가격이 0인 경우에도 계산이 가능하다")
        @Test
        void calculateTotalPrice_withZeroBasePrice() {
            // arrange
            ProductOptionModel option = ProductOptionFixture.createWithAdditionalPrice(new BigDecimal(3000));
            BigDecimal basePrice = new BigDecimal(0);

            // act
            BigDecimal totalPrice = option.calculateTotalPrice(basePrice);

            // assert
            assertThat(totalPrice).isEqualByComparingTo(new BigDecimal(3000));
        }

        @DisplayName("기본 가격이 음수면 예외가 발생한다")
        @Test
        void calculateTotalPrice_withNegativeBasePrice() {
            // arrange
            ProductOptionModel option = ProductOptionFixture.createProductOption();
            BigDecimal negativeBasePrice = new BigDecimal(-1000);

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                option.calculateTotalPrice(negativeBasePrice);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("기본 가격은 0보다 커야 합니다");
        }

        @DisplayName("큰 금액에 대해서도 정확히 계산된다")
        @Test
        void calculateTotalPrice_withLargeAmounts() {
            // arrange
            ProductOptionModel option = ProductOptionFixture.createWithAdditionalPrice(new BigDecimal(999999));
            BigDecimal basePrice = new BigDecimal(1000000);

            // act
            BigDecimal totalPrice = option.calculateTotalPrice(basePrice);

            // assert
            assertThat(totalPrice).isEqualByComparingTo(new BigDecimal(1999999));
        }
    }

    @Nested
    @DisplayName("유효성 검증 관련 테스트")
    class IsValidTest {

        @DisplayName("정상적인 옵션은 유효하다")
        @Test
        void isValid_withValidOption() {
            // arrange
            ProductOptionModel option = ProductOptionFixture.createProductOption();

            // act
            boolean isValid = option.isValid();

            // assert
            assertThat(isValid).isTrue();
        }

        @DisplayName("모든 필드가 null인 경우 유효하지 않다")
        @Test
        void isValid_withNullFields() {
            // arrange
            ProductOptionModel option = new ProductOptionModel();

            // act
            boolean isValid = option.isValid();

            // assert
            assertThat(isValid).isFalse();
        }

        @DisplayName("추가 가격이 0인 옵션도 유효하다")
        @Test
        void isValid_withZeroAdditionalPrice() {
            // arrange
            ProductOptionModel option = ProductOptionFixture.createFreeOption();

            // act
            boolean isValid = option.isValid();

            // assert
            assertThat(isValid).isTrue();
        }

        @DisplayName("추가 가격이 음수인 옵션도 유효하다")
        @Test
        void isValid_withNegativeAdditionalPrice() {
            // arrange
            ProductOptionModel option = ProductOptionFixture.createDiscountOption();

            // act
            boolean isValid = option.isValid();

            // assert
            assertThat(isValid).isTrue();
        }
    }

    @Nested
    @DisplayName("Fixture를 사용한 테스트")
    class FixtureTest {

        @DisplayName("기본 Fixture로 옵션을 생성할 수 있다")
        @Test
        void createWithDefaultFixture() {
            // act
            ProductOptionModel option = ProductOptionFixture.createProductOption();

            // assert
            assertAll(
                    () -> assertThat(option).isNotNull(),
                    () -> assertThat(option.getProductId().getValue()).isEqualTo(ProductOptionFixture.DEFAULT_PRODUCT_ID),
                    () -> assertThat(option.getName().getValue()).isEqualTo(ProductOptionFixture.DEFAULT_OPTION_NAME),
                    () -> assertThat(option.getValue().getValue()).isEqualTo(ProductOptionFixture.DEFAULT_OPTION_VALUE),
                    () -> assertThat(option.getAdditionalPrice().getValue()).isEqualTo(ProductOptionFixture.DEFAULT_ADDITIONAL_PRICE),
                    () -> assertThat(option.isValid()).isTrue()
            );
        }

        @DisplayName("사이즈 옵션을 생성할 수 있다")
        @Test
        void createSizeOption() {
            // arrange
            String size = "XL";
            BigDecimal additionalPrice = new BigDecimal(2000);

            // act
            ProductOptionModel option = ProductOptionFixture.createSizeOption(size, additionalPrice);

            // assert
            assertAll(
                    () -> assertThat(option.getName().getValue()).isEqualTo("사이즈"),
                    () -> assertThat(option.getValue().getValue()).isEqualTo(size),
                    () -> assertThat(option.getAdditionalPrice().getValue()).isEqualByComparingTo(additionalPrice),
                    () -> assertThat(option.isValid()).isTrue()
            );
        }

        @DisplayName("색상 옵션을 생성할 수 있다")
        @Test
        void createColorOption() {
            // arrange
            String color = "파란색";
            BigDecimal additionalPrice = new BigDecimal(1000);

            // act
            ProductOptionModel option = ProductOptionFixture.createColorOption(color, additionalPrice);

            // assert
            assertAll(
                    () -> assertThat(option.getName().getValue()).isEqualTo("색상"),
                    () -> assertThat(option.getValue().getValue()).isEqualTo(color),
                    () -> assertThat(option.getAdditionalPrice().getValue()).isEqualByComparingTo(additionalPrice),
                    () -> assertThat(option.isValid()).isTrue()
            );
        }

        @DisplayName("무료 옵션을 생성할 수 있다")
        @Test
        void createFreeOption() {
            // act
            ProductOptionModel option = ProductOptionFixture.createFreeOption();

            // assert
            assertAll(
                    () -> assertThat(option.getAdditionalPrice().getValue()).isZero(),
                    () -> assertThat(option.getAdditionalPrice().isZero()).isTrue(),
                    () -> assertThat(option.isValid()).isTrue()
            );
        }

        @DisplayName("할인 옵션을 생성할 수 있다")
        @Test
        void createDiscountOption() {
            // act
            ProductOptionModel option = ProductOptionFixture.createDiscountOption();

            // assert
            assertAll(
                    () -> assertThat(option.getAdditionalPrice().getValue()).isNegative(),
                    () -> assertThat(option.getAdditionalPrice().isNegative()).isTrue(),
                    () -> assertThat(option.isValid()).isTrue()
            );
        }
    }

    @Nested
    @DisplayName("실제 사용 시나리오 테스트")
    class RealWorldScenarioTest {

        @DisplayName("티셔츠 색상 옵션 시나리오")
        @Test
        void tshirtColorOptionScenario() {
            // arrange
            BigDecimal basePrice = new BigDecimal(25000);
            ProductOptionModel whiteOption = ProductOptionFixture.createColorOption("화이트", BigDecimal.ZERO);
            ProductOptionModel blackOption = ProductOptionFixture.createColorOption("블랙", new BigDecimal(2000));
            ProductOptionModel redOption = ProductOptionFixture.createColorOption("레드", new BigDecimal(3000));

            // act & assert
            assertAll(
                    () -> assertThat(whiteOption.calculateTotalPrice(basePrice)).isEqualByComparingTo(new BigDecimal(25000)),
                    () -> assertThat(blackOption.calculateTotalPrice(basePrice)).isEqualByComparingTo(new BigDecimal(27000)),
                    () -> assertThat(redOption.calculateTotalPrice(basePrice)).isEqualByComparingTo(new BigDecimal(28000)),
                    () -> assertThat(whiteOption.isValid()).isTrue(),
                    () -> assertThat(blackOption.isValid()).isTrue(),
                    () -> assertThat(redOption.isValid()).isTrue()
            );
        }

        @DisplayName("신발 사이즈 옵션 시나리오")
        @Test
        void shoeSizeOptionScenario() {
            // arrange
            BigDecimal basePrice = new BigDecimal(80000);
            ProductOptionModel size250 = ProductOptionFixture.createSizeOption("250", new BigDecimal(0));
            ProductOptionModel size260 = ProductOptionFixture.createSizeOption("260", new BigDecimal(0));
            ProductOptionModel size300 = ProductOptionFixture.createSizeOption("300", new BigDecimal(10000)); // 큰 사이즈 추가비용

            // act & assert
            assertAll(
                    () -> assertThat(size250.calculateTotalPrice(basePrice)).isEqualByComparingTo(new BigDecimal(80000)),
                    () -> assertThat(size260.calculateTotalPrice(basePrice)).isEqualByComparingTo(new BigDecimal(80000)),
                    () -> assertThat(size300.calculateTotalPrice(basePrice)).isEqualByComparingTo(new BigDecimal(90000)),
                    () -> assertThat(size250.isValid()).isTrue(),
                    () -> assertThat(size260.isValid()).isTrue(),
                    () -> assertThat(size300.isValid()).isTrue()
            );
        }

        @DisplayName("할인 이벤트 옵션 시나리오")
        @Test
        void discountEventOptionScenario() {
            // arrange
            BigDecimal basePrice = new BigDecimal(50000);
            ProductOptionModel memberDiscount = ProductOptionFixture.createProductOption(1L, "회원할인", "10% 할인", new BigDecimal(-5000));
            ProductOptionModel vipDiscount = ProductOptionFixture.createProductOption(1L, "VIP할인", "20% 할인", new BigDecimal(-10000));

            // act & assert
            assertAll(
                    () -> assertThat(memberDiscount.calculateTotalPrice(basePrice)).isEqualByComparingTo(new BigDecimal(45000)),
                    () -> assertThat(vipDiscount.calculateTotalPrice(basePrice)).isEqualByComparingTo(new BigDecimal(40000)),
                    () -> assertThat(memberDiscount.isValid()).isTrue(),
                    () -> assertThat(vipDiscount.isValid()).isTrue()
            );
        }

        @DisplayName("복합 옵션 조합 시나리오")
        @Test
        void multipleOptionsScenario() {
            // arrange
            BigDecimal basePrice = new BigDecimal(30000);
            ProductOptionModel sizeOption = ProductOptionFixture.createSizeOption("L", new BigDecimal(2000));
            ProductOptionModel colorOption = ProductOptionFixture.createColorOption("프리미엄 블랙", new BigDecimal(5000));

            // act
            BigDecimal sizeAppliedPrice = sizeOption.calculateTotalPrice(basePrice);
            BigDecimal finalPrice = colorOption.calculateTotalPrice(sizeAppliedPrice);

            // assert
            assertThat(finalPrice).isEqualByComparingTo(new BigDecimal(37000));
        }
    }
}
