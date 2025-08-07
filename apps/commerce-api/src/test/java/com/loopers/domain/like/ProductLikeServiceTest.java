package com.loopers.domain.like;

import com.loopers.domain.like.product.ProductLikeModel;
import com.loopers.domain.like.product.ProductLikeService;
import com.loopers.domain.product.ProductModel;
import com.loopers.domain.product.ProductFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("상품 좋아요 도메인 서비스 테스트")
class ProductLikeServiceTest {

    private final ProductLikeService productLikeService = new ProductLikeService();

    @Nested
    @DisplayName("상품 좋아요 등록 테스트")
    class AddLikeTest {

        @DisplayName("상품 좋아요 등록 시 좋아요 생성과 상품 likeCount가 증가한다")
        @Test
        void addLike_createsLikeAndIncrementsCount() {
            // arrange
            Long userId = 1L;
            ProductModel product = ProductFixture.createProductWithLikeCount(new BigDecimal("5"));
            Long productId = product.getId();

            // act
            ProductLikeModel result = productLikeService.addLike(product, userId);

            // assert
            assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.getUserId()).isEqualTo(userId),
                    () -> assertThat(result.getProductId()).isEqualTo(productId),
                    () -> assertThat(product.getLikeCount().getValue()).isEqualByComparingTo(new BigDecimal("6"))
            );
        }

        @DisplayName("좋아요 수가 0인 상품도 정상적으로 증가한다")
        @Test
        void addLike_actZeroCount_increments() {
            // arrange
            Long userId = 1L;
            ProductModel product = ProductFixture.createProductWithLikeCount(BigDecimal.ZERO);

            // act
            ProductLikeModel result = productLikeService.addLike(product, userId);

            // assert
            assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(product.getLikeCount().getValue()).isEqualByComparingTo(BigDecimal.ONE)
            );
        }
    }

    @Nested
    @DisplayName("상품 좋아요 취소 테스트")
    class RemoveLikeTest {

        @DisplayName("상품 좋아요 취소 시 상품 likeCount가 감소한다")
        @Test
        void removeLike_decrementsCount() {
            // arrange
            Long userId = 1L;
            ProductModel product = ProductFixture.createProductWithLikeCount(new BigDecimal("5"));
            ProductLikeModel existingLike = ProductLikeModel.create(userId, product.getId());

            // act
            productLikeService.removeLike(product, existingLike);

            // assert
            assertThat(product.getLikeCount().getValue()).isEqualByComparingTo(new BigDecimal("4"));
        }

        @DisplayName("좋아요 수가 0일 때 감소해도 음수가 되지 않는다")
        @Test
        void removeLike_actZeroCount_staysZero() {
            // arrange
            Long userId = 1L;
            ProductModel product = ProductFixture.createProductWithLikeCount(BigDecimal.ZERO);
            ProductLikeModel existingLike = ProductLikeModel.create(userId, product.getId());

            // act
            productLikeService.removeLike(product, existingLike);

            // assert
            assertThat(product.getLikeCount().getValue()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("상품 좋아요 토글 테스트")
    class ToggleLikeTest {

        @DisplayName("좋아요가 없을 때 토글하면 좋아요가 추가되고 카운트가 증가한다")
        @Test
        void toggleLike_actNotExists_addsLikeAndIncrementsCount() {
            // arrange
            Long userId = 1L;
            ProductModel product = ProductFixture.createProductWithLikeCount(new BigDecimal("3"));
            ProductLikeModel existingLike = null;

            // act
            ProductLikeService.LikeToggleResult result = productLikeService.toggleLike(product, userId, existingLike);

            // assert
            assertAll(
                    () -> assertThat(result.isAdded()).isTrue(),
                    () -> assertThat(result.getLike()).isNotNull(),
                    () -> assertThat(result.getLike().getUserId()).isEqualTo(userId),
                    () -> assertThat(result.getLike().getProductId()).isEqualTo(product.getId()),
                    () -> assertThat(product.getLikeCount().getValue()).isEqualByComparingTo(new BigDecimal("4"))
            );
        }

        @DisplayName("좋아요가 있을 때 토글하면 좋아요가 제거되고 카운트가 감소한다")
        @Test
        void toggleLike_actExists_removesLikeAndDecrementsCount() {
            // arrange
            Long userId = 1L;
            ProductModel product = ProductFixture.createProductWithLikeCount(new BigDecimal("5"));
            ProductLikeModel existingLike = ProductLikeModel.create(userId, product.getId());

            // act
            ProductLikeService.LikeToggleResult result = productLikeService.toggleLike(product, userId, existingLike);

            // assert
            assertAll(
                    () -> assertThat(result.isRemoved()).isTrue(),
                    () -> assertThat(result.getLike()).isEqualTo(existingLike),
                    () -> assertThat(product.getLikeCount().getValue()).isEqualByComparingTo(new BigDecimal("4"))
            );
        }

        @DisplayName("두 번 토글하면 원래 상태로 돌아온다")
        @Test
        void toggleLike_twice_returnsToOriginalState() {
            // arrange
            Long userId = 1L;
            ProductModel product = ProductFixture.createProductWithLikeCount(new BigDecimal("10"));
            BigDecimal originalCount = product.getLikeCount().getValue();

            // act - 첫 번째 토글 (추가)
            ProductLikeService.LikeToggleResult result1 = productLikeService.toggleLike(product, userId, null);

            // act - 두 번째 토글 (제거)
            ProductLikeService.LikeToggleResult result2 = productLikeService.toggleLike(product, userId, result1.getLike());

            // assert
            assertAll(
                    () -> assertThat(result1.isAdded()).isTrue(),
                    () -> assertThat(result2.isRemoved()).isTrue(),
                    () -> assertThat(product.getLikeCount().getValue()).isEqualByComparingTo(originalCount)
            );
        }
    }

    @Nested
    @DisplayName("상품 좋아요 비즈니스 로직 통합 테스트")
    class BusinessLogicIntegrationTest {

        @DisplayName("여러 사용자의 좋아요 등록으로 상품 likeCount가 누적 증가한다")
        @Test
        void multipleUsersLike_accumulatesCount() {
            // arrange
            ProductModel product = ProductFixture.createProductWithLikeCount(BigDecimal.ZERO);
            Long user1Id = 1L;
            Long user2Id = 2L;
            Long user3Id = 3L;

            // act
            productLikeService.addLike(product, user1Id);
            productLikeService.addLike(product, user2Id);
            productLikeService.addLike(product, user3Id);

            // assert
            assertThat(product.getLikeCount().getValue()).isEqualByComparingTo(new BigDecimal("3"));
        }

        @DisplayName("좋아요 등록과 취소를 반복해도 정확한 카운트를 유지한다")
        @Test
        void addAndRemove_maintainsCorrectCount() {
            // arrange
            ProductModel product = ProductFixture.createProductWithLikeCount(new BigDecimal("5"));
            Long userId = 1L;

            // act & assert
            // 좋아요 추가
            ProductLikeModel like = productLikeService.addLike(product, userId);
            assertThat(product.getLikeCount().getValue()).isEqualByComparingTo(new BigDecimal("6"));

            // 좋아요 제거
            productLikeService.removeLike(product, like);
            assertThat(product.getLikeCount().getValue()).isEqualByComparingTo(new BigDecimal("5"));

            // 다시 좋아요 추가
            ProductLikeModel newLike = productLikeService.addLike(product, userId);
            assertThat(product.getLikeCount().getValue()).isEqualByComparingTo(new BigDecimal("6"));
        }

        @DisplayName("상품 좋아요 서비스는 상태를 가지지 않는다")
        @Test
        void productLikeService_isStateless() {
            // arrange
            ProductModel product1 = ProductFixture.createProductWithLikeCount(new BigDecimal("1"));
            ProductModel product2 = ProductFixture.createProductWithLikeCount(new BigDecimal("10"));
            Long userId = 1L;

            // act - 동일한 서비스 인스턴스로 서로 다른 상품 처리
            productLikeService.addLike(product1, userId);
            productLikeService.addLike(product2, userId);

            // assert - 각각 독립적으로 처리됨
            assertAll(
                    () -> assertThat(product1.getLikeCount().getValue()).isEqualByComparingTo(new BigDecimal("2")),
                    () -> assertThat(product2.getLikeCount().getValue()).isEqualByComparingTo(new BigDecimal("11"))
            );
        }
    }
}
