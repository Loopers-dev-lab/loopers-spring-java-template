package com.loopers.domain.like;

import com.loopers.domain.like.fixture.ProductLikeFixture;
import com.loopers.domain.like.product.ProductLikeModel;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductLikeModelTest {

    @Nested
    @DisplayName("상품 좋아요 생성 관련 테스트")
    class CreateTest {

        @DisplayName("정상적인 값으로 상품 좋아요를 생성할 수 있다")
        @Test
        void create_withValidValues() {
            // arrange
            Long userId = 1L;
            Long productId = 1L;
            LocalDateTime beforeCreation = LocalDateTime.now();

            // act
            ProductLikeModel productLike = ProductLikeModel.create(userId, productId);

            // assert
            assertAll(
                    () -> assertThat(productLike).isNotNull(),
                    () -> assertThat(productLike.getUserId()).isEqualTo(userId),
                    () -> assertThat(productLike.getProductId()).isEqualTo(productId),
                    () -> assertThat(productLike.getLikedAt()).isAfter(beforeCreation)
            );
        }

        @DisplayName("사용자 ID가 null이면 생성에 실패한다")
        @Test
        void create_whenUserIdNull() {
            // arrange
            Long userId = null;
            Long productId = 1L;
            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                ProductLikeModel.create(userId, productId);

            });
            //assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("상품 ID가 null이면 생성에 실패한다")
        @Test
        void create_whenProductIdNull() {
            // arrange
            Long userId = 1L;
            Long productId = null;

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                ProductLikeModel.create(userId, productId);
            });
            //assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("사용자 ID와 상품 ID가 모두 null이면 생성에 실패한다")
        @Test
        void create_whenBothIdsNull() {
            // arrange
            Long userId = null;
            Long productId = null;
            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                ProductLikeModel.create(userId, productId);
            });
            //assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("상품 좋아요 조회 관련 테스트")
    class QueryTest {

        @DisplayName("특정 사용자에게 속하는 좋아요인지 확인할 수 있다")
        @Test
        void belongsToUser_withCorrectUserId() {
            // arrange
            Long userId = 1L;
            Long productId = 1L;
            ProductLikeModel productLike = ProductLikeFixture.createProductLikeModel(userId, productId);

            // act & assert
            assertThat(productLike.belongsToUser(userId)).isTrue();
            assertThat(productLike.belongsToUser(2L)).isFalse();
        }

        @DisplayName("특정 상품에 대한 좋아요인지 확인할 수 있다")
        @Test
        void isForProduct_withCorrectProductId() {
            // arrange
            Long userId = 1L;
            Long productId = 1L;
            ProductLikeModel productLike = ProductLikeFixture.createProductLikeModel(userId, productId);

            // act & assert
            assertThat(productLike.isForProduct(productId)).isTrue();
            assertThat(productLike.isForProduct(2L)).isFalse();
        }

        @DisplayName("null 사용자 ID로 조회하면 false를 반환한다")
        @Test
        void belongsToUser_withNullUserId() {
            // arrange
            ProductLikeModel productLike = ProductLikeFixture.createProductLikeModel();

            // act & assert
            assertThat(productLike.belongsToUser(null)).isFalse();
        }

        @DisplayName("null 상품 ID로 조회하면 false를 반환한다")
        @Test
        void isForProduct_withNullProductId() {
            // arrange
            ProductLikeModel productLike = ProductLikeFixture.createProductLikeModel();

            // act & assert
            assertThat(productLike.isForProduct(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("상품 좋아요 비즈니스 로직 테스트")
    class BusinessLogicTest {

        @DisplayName("좋아요 생성 시 현재 시간이 설정된다")
        @Test
        void create_setsCurrentTime() {
            // arrange
            LocalDateTime beforeCreation = LocalDateTime.now();

            // act
            ProductLikeModel productLike = ProductLikeFixture.createProductLikeModel();
            LocalDateTime afterCreation = LocalDateTime.now();

            // assert
            assertAll(
                    () -> assertThat(productLike.getLikedAt()).isAfter(beforeCreation.minusSeconds(1)),
                    () -> assertThat(productLike.getLikedAt()).isBefore(afterCreation.plusSeconds(1))
            );
        }

        @DisplayName("동일한 사용자와 상품으로 생성된 좋아요는 같은 값을 가진다")
        @Test
        void create_withSameParameters() {
            // arrange
            Long userId = 1L;
            Long productId = 1L;

            // act
            ProductLikeModel like1 = ProductLikeModel.create(userId, productId);
            ProductLikeModel like2 = ProductLikeModel.create(userId, productId);

            // assert
            assertAll(
                    () -> assertThat(like1.getUserId()).isEqualTo(like2.getUserId()),
                    () -> assertThat(like1.getProductId()).isEqualTo(like2.getProductId()),
                    () -> assertThat(like1.belongsToUser(userId)).isTrue(),
                    () -> assertThat(like2.belongsToUser(userId)).isTrue(),
                    () -> assertThat(like1.isForProduct(productId)).isTrue(),
                    () -> assertThat(like2.isForProduct(productId)).isTrue()
            );
        }
    }

    @Nested
    @DisplayName("Fixture를 사용한 테스트")
    class FixtureTest {

        @DisplayName("기본 Fixture로 생성할 수 있다")
        @Test
        void createWithDefaultFixture() {
            // act
            ProductLikeModel productLike = ProductLikeFixture.createProductLikeModel();

            // assert
            assertAll(
                    () -> assertThat(productLike).isNotNull(),
                    () -> assertThat(productLike.getUserId()).isEqualTo(ProductLikeFixture.DEFAULT_USER_ID),
                    () -> assertThat(productLike.getProductId()).isEqualTo(ProductLikeFixture.DEFAULT_PRODUCT_ID)
            );
        }

        @DisplayName("특정 사용자 ID로 Fixture를 생성할 수 있다")
        @Test
        void createWithSpecificUserId() {
            // arrange
            Long userId = 100L;

            // act
            ProductLikeModel productLike = ProductLikeFixture.createProductLikeWithUserId(userId);

            // assert
            assertAll(
                    () -> assertThat(productLike.getUserId()).isEqualTo(userId),
                    () -> assertThat(productLike.getProductId()).isEqualTo(ProductLikeFixture.DEFAULT_PRODUCT_ID)
            );
        }

        @DisplayName("특정 상품 ID로 Fixture를 생성할 수 있다")
        @Test
        void createWithSpecificProductId() {
            // arrange
            Long productId = 200L;

            // act
            ProductLikeModel productLike = ProductLikeFixture.createProductLikeWithProductId(productId);

            // assert
            assertAll(
                    () -> assertThat(productLike.getUserId()).isEqualTo(ProductLikeFixture.DEFAULT_USER_ID),
                    () -> assertThat(productLike.getProductId()).isEqualTo(productId)
            );
        }
    }
}
