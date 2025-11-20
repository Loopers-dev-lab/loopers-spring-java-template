package com.loopers.domain.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.loopers.domain.product.vo.Stock;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class ProductTest {

    @DisplayName("상품을 생성할 때, ")
    @Nested
    class Create {

        @Test
        void 상품_이름_가격_재고수량_브랜드가_주어지면_정상적으로_생성된다() {
            // arrange
            Long brandId = 1L;
            String name = "test";
            Integer price = 2000;
            long amount = 200L;

            // act
            Product product = Product.create(name, price, new Stock(amount), brandId);

            // assert
            assertAll(
                    () -> assertThat(product.getId()).isNotNull(),
                    () -> assertThat(product.getName()).isEqualTo("test"),
                    () -> assertThat(product.getPrice()).isEqualTo(2000),
                    () -> assertThat(product.getStock().getQuantity()).isEqualTo(200L),
                    () -> assertThat(product.getLikeCount().getValue()).isEqualTo(0),
                    () -> assertThat(product.getBrandId()).isEqualTo(1L)
            );
        }

        @NullAndEmptySource
        @ParameterizedTest
        void 이름이_null이거나_비어있으면_BadRequest_예외_발생한다(String emptyName) {
            // arrange
            Long brandId = 1L;
            Integer price = 2000;
            long amount = 100L;

            // act & assert
            assertThatThrownBy(() -> Product.create(emptyName, price, new Stock(amount), brandId))
                    .isInstanceOfSatisfying(CoreException.class, e -> {
                        assertThat(e.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
                        assertThat(e.getCustomMessage()).isEqualTo("이름은 비어있을 수 없습니다.");
                    });
        }

        @NullSource
        @ValueSource(ints = {-1})
        @ParameterizedTest
        void 가격이_null이거나_음수면_BadRequest_예외_발생한다(Integer price) {
            // arrange
            Long brandId = 1L;
            String name = "test";
            long amount = 100L;

            // act & assert
            assertThatThrownBy(() -> Product.create(name, price, new Stock(amount), brandId))
                    .isInstanceOfSatisfying(CoreException.class, e -> {
                        assertThat(e.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
                        assertThat(e.getCustomMessage()).isEqualTo("가격은 0 이상이어야 합니다.");
                    });
        }

        @Test
        void 브랜드_PK_ID가_null이면_BadRequest_예외_발생한다() {
            // arrange
            Long brandId = null;
            String name = "test";
            Integer price = 2000;
            long amount = 100L;

            // act & assert
            assertThatThrownBy(() -> Product.create(name, price, new Stock(amount), brandId))
                    .isInstanceOfSatisfying(CoreException.class, e -> {
                        assertThat(e.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
                        assertThat(e.getCustomMessage()).isEqualTo("브랜드는 비어있을 수 없습니다.");
                    });
        }
    }
}
