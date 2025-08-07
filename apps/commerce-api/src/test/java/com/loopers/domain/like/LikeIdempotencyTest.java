package com.loopers.domain.like;

import com.loopers.domain.like.brand.BrandLikeModel;
import com.loopers.domain.like.product.ProductLikeModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("좋아요 멱등성 처리 통합 테스트")
class LikeIdempotencyTest {

    @Nested
    @DisplayName("상품 좋아요 멱등성 테스트")
    class ProductLikeIdempotencyTest {

        @DisplayName("동일한 파라미터로 여러 번 좋아요 생성해도 같은 결과를 반환한다")
        @Test
        void createProductLike_multipleCallsWithSameParameters_idempotent() {
            // arrange
            Long userId = 1L;
            Long productId = 100L;

            // act - 동일한 파라미터로 여러 번 생성
            ProductLikeModel like1 = ProductLikeModel.create(userId, productId);
            ProductLikeModel like2 = ProductLikeModel.create(userId, productId);
            ProductLikeModel like3 = ProductLikeModel.create(userId, productId);

            // assert - 모든 좋아요는 동일한 속성을 가짐
            assertAll(
                    () -> assertThat(like1.getUserId()).isEqualTo(like2.getUserId()),
                    () -> assertThat(like1.getProductId()).isEqualTo(like2.getProductId()),
                    () -> assertThat(like1.getUserId()).isEqualTo(like3.getUserId()),
                    () -> assertThat(like1.getProductId()).isEqualTo(like3.getProductId()),
                    () -> assertThat(like1.belongsToUser(userId)).isTrue(),
                    () -> assertThat(like2.belongsToUser(userId)).isTrue(),
                    () -> assertThat(like3.belongsToUser(userId)).isTrue(),
                    () -> assertThat(like1.isForProduct(productId)).isTrue(),
                    () -> assertThat(like2.isForProduct(productId)).isTrue(),
                    () -> assertThat(like3.isForProduct(productId)).isTrue()
            );
        }

        @DisplayName("서로 다른 사용자의 좋아요는 독립적으로 생성된다")
        @Test
        void createProductLike_differentUsers_independent() {
            // arrange
            Long user1Id = 1L;
            Long user2Id = 2L;
            Long productId = 100L;

            // act
            ProductLikeModel like1 = ProductLikeModel.create(user1Id, productId);
            ProductLikeModel like2 = ProductLikeModel.create(user2Id, productId);

            // assert
            assertAll(
                    () -> assertThat(like1.belongsToUser(user1Id)).isTrue(),
                    () -> assertThat(like1.belongsToUser(user2Id)).isFalse(),
                    () -> assertThat(like2.belongsToUser(user2Id)).isTrue(),
                    () -> assertThat(like2.belongsToUser(user1Id)).isFalse(),
                    () -> assertThat(like1.isForProduct(productId)).isTrue(),
                    () -> assertThat(like2.isForProduct(productId)).isTrue()
            );
        }

        @DisplayName("동일 사용자의 서로 다른 상품 좋아요는 독립적으로 생성된다")
        @Test
        void createProductLike_differentProducts_independent() {
            // arrange
            Long userId = 1L;
            Long product1Id = 100L;
            Long product2Id = 200L;

            // act
            ProductLikeModel like1 = ProductLikeModel.create(userId, product1Id);
            ProductLikeModel like2 = ProductLikeModel.create(userId, product2Id);

            // assert
            assertAll(
                    () -> assertThat(like1.belongsToUser(userId)).isTrue(),
                    () -> assertThat(like2.belongsToUser(userId)).isTrue(),
                    () -> assertThat(like1.isForProduct(product1Id)).isTrue(),
                    () -> assertThat(like1.isForProduct(product2Id)).isFalse(),
                    () -> assertThat(like2.isForProduct(product2Id)).isTrue(),
                    () -> assertThat(like2.isForProduct(product1Id)).isFalse()
            );
        }
    }

    @Nested
    @DisplayName("브랜드 좋아요 멱등성 테스트")
    class BrandLikeIdempotencyTest {

        @DisplayName("동일한 파라미터로 여러 번 좋아요 생성해도 같은 결과를 반환한다")
        @Test
        void createBrandLike_multipleCallsWithSameParameters_idempotent() {
            // arrange
            Long userId = 1L;
            Long brandId = 100L;

            // act - 동일한 파라미터로 여러 번 생성
            BrandLikeModel like1 = BrandLikeModel.create(userId, brandId);
            BrandLikeModel like2 = BrandLikeModel.create(userId, brandId);
            BrandLikeModel like3 = BrandLikeModel.create(userId, brandId);

            // assert - 모든 좋아요는 동일한 속성을 가짐
            assertAll(
                    () -> assertThat(like1.getUserId()).isEqualTo(like2.getUserId()),
                    () -> assertThat(like1.getBrandId()).isEqualTo(like2.getBrandId()),
                    () -> assertThat(like1.getUserId()).isEqualTo(like3.getUserId()),
                    () -> assertThat(like1.getBrandId()).isEqualTo(like3.getBrandId()),
                    () -> assertThat(like1.belongsToUser(userId)).isTrue(),
                    () -> assertThat(like2.belongsToUser(userId)).isTrue(),
                    () -> assertThat(like3.belongsToUser(userId)).isTrue(),
                    () -> assertThat(like1.isForBrand(brandId)).isTrue(),
                    () -> assertThat(like2.isForBrand(brandId)).isTrue(),
                    () -> assertThat(like3.isForBrand(brandId)).isTrue()
            );
        }

        @DisplayName("서로 다른 사용자의 좋아요는 독립적으로 생성된다")
        @Test
        void createBrandLike_differentUsers_independent() {
            // arrange
            Long user1Id = 1L;
            Long user2Id = 2L;
            Long brandId = 100L;

            // act
            BrandLikeModel like1 = BrandLikeModel.create(user1Id, brandId);
            BrandLikeModel like2 = BrandLikeModel.create(user2Id, brandId);

            // assert
            assertAll(
                    () -> assertThat(like1.belongsToUser(user1Id)).isTrue(),
                    () -> assertThat(like1.belongsToUser(user2Id)).isFalse(),
                    () -> assertThat(like2.belongsToUser(user2Id)).isTrue(),
                    () -> assertThat(like2.belongsToUser(user1Id)).isFalse(),
                    () -> assertThat(like1.isForBrand(brandId)).isTrue(),
                    () -> assertThat(like2.isForBrand(brandId)).isTrue()
            );
        }

        @DisplayName("동일 사용자의 서로 다른 브랜드 좋아요는 독립적으로 생성된다")
        @Test
        void createBrandLike_differentBrands_independent() {
            // arrange
            Long userId = 1L;
            Long brand1Id = 100L;
            Long brand2Id = 200L;

            // act
            BrandLikeModel like1 = BrandLikeModel.create(userId, brand1Id);
            BrandLikeModel like2 = BrandLikeModel.create(userId, brand2Id);

            // assert
            assertAll(
                    () -> assertThat(like1.belongsToUser(userId)).isTrue(),
                    () -> assertThat(like2.belongsToUser(userId)).isTrue(),
                    () -> assertThat(like1.isForBrand(brand1Id)).isTrue(),
                    () -> assertThat(like1.isForBrand(brand2Id)).isFalse(),
                    () -> assertThat(like2.isForBrand(brand2Id)).isTrue(),
                    () -> assertThat(like2.isForBrand(brand1Id)).isFalse()
            );
        }
    }

    @Nested
    @DisplayName("좋아요 비즈니스 로직 멱등성 검증")
    class LikeBusinessLogicIdempotencyTest {

        @DisplayName("상품 좋아요 생성은 항상 현재 시간을 설정한다")
        @Test
        void productLikeCreation_alwaysSetsCurrentTime() {
            // arrange
            Long userId = 1L;
            Long productId = 100L;

            // act
            ProductLikeModel like1 = ProductLikeModel.create(userId, productId);
            ProductLikeModel like2 = ProductLikeModel.create(userId, productId);

            // assert - 각각 생성 시점의 현재 시간이 설정됨
            assertAll(
                    () -> assertThat(like1.getLikedAt()).isNotNull(),
                    () -> assertThat(like2.getLikedAt()).isNotNull(),
                    () -> assertThat(like1.getLikedAt()).isBeforeOrEqualTo(like2.getLikedAt())
            );
        }

        @DisplayName("브랜드 좋아요 생성은 항상 현재 시간을 설정한다")
        @Test
        void brandLikeCreation_alwaysSetsCurrentTime() {
            // arrange
            Long userId = 1L;
            Long brandId = 100L;

            // act
            BrandLikeModel like1 = BrandLikeModel.create(userId, brandId);
            BrandLikeModel like2 = BrandLikeModel.create(userId, brandId);

            // assert - 각각 생성 시점의 현재 시간이 설정됨
            assertAll(
                    () -> assertThat(like1.getLikedAt()).isNotNull(),
                    () -> assertThat(like2.getLikedAt()).isNotNull(),
                    () -> assertThat(like1.getLikedAt()).isBeforeOrEqualTo(like2.getLikedAt())
            );
        }

        @DisplayName("좋아요 조회 메서드들은 멱등성을 보장한다")
        @Test
        void likeQueryMethods_areIdempotent() {
            // arrange
            Long userId = 1L;
            Long productId = 100L;
            Long brandId = 200L;

            ProductLikeModel productLike = ProductLikeModel.create(userId, productId);
            BrandLikeModel brandLike = BrandLikeModel.create(userId, brandId);

            // act & assert - 여러 번 호출해도 같은 결과
            for (int i = 0; i < 3; i++) {
                assertAll(
                        () -> assertThat(productLike.belongsToUser(userId)).isTrue(),
                        () -> assertThat(productLike.belongsToUser(999L)).isFalse(),
                        () -> assertThat(productLike.isForProduct(productId)).isTrue(),
                        () -> assertThat(productLike.isForProduct(999L)).isFalse(),
                        () -> assertThat(brandLike.belongsToUser(userId)).isTrue(),
                        () -> assertThat(brandLike.belongsToUser(999L)).isFalse(),
                        () -> assertThat(brandLike.isForBrand(brandId)).isTrue(),
                        () -> assertThat(brandLike.isForBrand(999L)).isFalse()
                );
            }
        }
    }
}
