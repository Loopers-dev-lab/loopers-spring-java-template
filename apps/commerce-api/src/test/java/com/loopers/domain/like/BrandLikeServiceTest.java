package com.loopers.domain.like;

import com.loopers.domain.brand.BrandModel;
import com.loopers.domain.brand.BrandFixture;
import com.loopers.domain.like.brand.BrandLikeModel;
import com.loopers.domain.like.brand.BrandLikeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("브랜드 좋아요 도메인 서비스 테스트")
class BrandLikeServiceTest {

    private final BrandLikeService brandLikeService = new BrandLikeService();

    @Nested
    @DisplayName("브랜드 좋아요 등록 테스트")
    class AddLikeTest {

        @DisplayName("브랜드 좋아요 등록 시 좋아요 생성과 브랜드 likeCount가 증가한다")
        @Test
        void addLike_createsLikeAndIncrementsCount() {
            // arrange
            Long userId = 1L;
            BrandModel brand = BrandFixture.createBrandWithLikeCount(new BigDecimal("5"));
            Long brandId = brand.getId();

            // act
            BrandLikeModel result = brandLikeService.addLike(brand, userId);

            // assert
            assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.getUserId()).isEqualTo(userId),
                    () -> assertThat(result.getBrandId()).isEqualTo(brandId),
                    () -> assertThat(brand.getLikeCount().getBrandLikeCount()).isEqualByComparingTo(new BigDecimal("6"))
            );
        }

        @DisplayName("좋아요 수가 0인 브랜드도 정상적으로 증가한다")
        @Test
        void addLike_whenZeroCount_increments() {
            // arrange
            Long userId = 1L;
            BrandModel brand = BrandFixture.createBrandWithLikeCount(BigDecimal.ZERO);

            // act
            BrandLikeModel result = brandLikeService.addLike(brand, userId);

            // assert
            assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(brand.getLikeCount().getBrandLikeCount()).isEqualByComparingTo(BigDecimal.ONE)
            );
        }
    }

    @Nested
    @DisplayName("브랜드 좋아요 취소 테스트")
    class RemoveLikeTest {

        @DisplayName("브랜드 좋아요 취소 시 브랜드 likeCount가 감소한다")
        @Test
        void removeLike_decrementsCount() {
            // arrange
            Long userId = 1L;
            BrandModel brand = BrandFixture.createBrandWithLikeCount(new BigDecimal("5"));
            BrandLikeModel existingLike = BrandLikeModel.create(userId, brand.getId());

            // act
            brandLikeService.removeLike(brand, existingLike);

            // assert
            assertThat(brand.getLikeCount().getBrandLikeCount()).isEqualByComparingTo(new BigDecimal("4"));
        }

        @DisplayName("좋아요 수가 0일 때 감소해도 음수가 되지 않는다")
        @Test
        void removeLike_whenZeroCount_staysZero() {
            // arrange
            Long userId = 1L;
            BrandModel brand = BrandFixture.createBrandWithLikeCount(BigDecimal.ZERO);
            BrandLikeModel existingLike = BrandLikeModel.create(userId, brand.getId());

            // act
            brandLikeService.removeLike(brand, existingLike);

            // assert
            assertThat(brand.getLikeCount().getBrandLikeCount()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("브랜드 좋아요 토글 테스트")
    class ToggleLikeTest {

        @DisplayName("좋아요가 없을 때 토글하면 좋아요가 추가되고 카운트가 증가한다")
        @Test
        void toggleLike_whenNotExists_addsLikeAndIncrementsCount() {
            // arrange
            Long userId = 1L;
            BrandModel brand = BrandFixture.createBrandWithLikeCount(new BigDecimal("3"));
            BrandLikeModel existingLike = null;

            // act
            BrandLikeService.LikeToggleResult result = brandLikeService.toggleLike(brand, userId, existingLike);

            // assert
            assertAll(
                    () -> assertThat(result.isAdded()).isTrue(),
                    () -> assertThat(result.getLike()).isNotNull(),
                    () -> assertThat(result.getLike().getUserId()).isEqualTo(userId),
                    () -> assertThat(result.getLike().getBrandId()).isEqualTo(brand.getId()),
                    () -> assertThat(brand.getLikeCount().getBrandLikeCount()).isEqualByComparingTo(new BigDecimal("4"))
            );
        }

        @DisplayName("좋아요가 있을 때 토글하면 좋아요가 제거되고 카운트가 감소한다")
        @Test
        void toggleLike_whenExists_removesLikeAndDecrementsCount() {
            // arrange
            Long userId = 1L;
            BrandModel brand = BrandFixture.createBrandWithLikeCount(new BigDecimal("5"));
            BrandLikeModel existingLike = BrandLikeModel.create(userId, brand.getId());

            // act
            BrandLikeService.LikeToggleResult result = brandLikeService.toggleLike(brand, userId, existingLike);

            // assert
            assertAll(
                    () -> assertThat(result.isRemoved()).isTrue(),
                    () -> assertThat(result.getLike()).isEqualTo(existingLike),
                    () -> assertThat(brand.getLikeCount().getBrandLikeCount()).isEqualByComparingTo(new BigDecimal("4"))
            );
        }

        @DisplayName("두 번 토글하면 원래 상태로 돌아온다")
        @Test
        void toggleLike_twice_returnsToOriginalState() {
            // arrange
            Long userId = 1L;
            BrandModel brand = BrandFixture.createBrandWithLikeCount(new BigDecimal("10"));
            BigDecimal originalCount = brand.getLikeCount().getBrandLikeCount();

            // act - 첫 번째 토글 (추가)
            BrandLikeService.LikeToggleResult result1 = brandLikeService.toggleLike(brand, userId, null);

            // act - 두 번째 토글 (제거)
            BrandLikeService.LikeToggleResult result2 = brandLikeService.toggleLike(brand, userId, result1.getLike());

            // assert
            assertAll(
                    () -> assertThat(result1.isAdded()).isTrue(),
                    () -> assertThat(result2.isRemoved()).isTrue(),
                    () -> assertThat(brand.getLikeCount().getBrandLikeCount()).isEqualByComparingTo(originalCount)
            );
        }
    }

    @Nested
    @DisplayName("브랜드 좋아요 비즈니스 로직 통합 테스트")
    class BusinessLogicIntegrationTest {

        @DisplayName("여러 사용자의 좋아요 등록으로 브랜드 likeCount가 누적 증가한다")
        @Test
        void multipleUsersLike_accumulatesCount() {
            // arrange
            BrandModel brand = BrandFixture.createBrandWithLikeCount(BigDecimal.ZERO);
            Long user1Id = 1L;
            Long user2Id = 2L;
            Long user3Id = 3L;

            // act
            brandLikeService.addLike(brand, user1Id);
            brandLikeService.addLike(brand, user2Id);
            brandLikeService.addLike(brand, user3Id);

            // assert
            assertThat(brand.getLikeCount().getBrandLikeCount()).isEqualByComparingTo(new BigDecimal("3"));
        }

        @DisplayName("좋아요 등록과 취소를 반복해도 정확한 카운트를 유지한다")
        @Test
        void addAndRemove_maintainsCorrectCount() {
            // arrange
            BrandModel brand = BrandFixture.createBrandWithLikeCount(new BigDecimal("5"));
            Long userId = 1L;

            // act & then
            // 좋아요 추가
            BrandLikeModel like = brandLikeService.addLike(brand, userId);
            assertThat(brand.getLikeCount().getBrandLikeCount()).isEqualByComparingTo(new BigDecimal("6"));

            // 좋아요 제거
            brandLikeService.removeLike(brand, like);
            assertThat(brand.getLikeCount().getBrandLikeCount()).isEqualByComparingTo(new BigDecimal("5"));

            // 다시 좋아요 추가
            BrandLikeModel newLike = brandLikeService.addLike(brand, userId);
            assertThat(brand.getLikeCount().getBrandLikeCount()).isEqualByComparingTo(new BigDecimal("6"));
        }

        @DisplayName("브랜드 좋아요 서비스는 상태를 가지지 않는다")
        @Test
        void brandLikeService_isStateless() {
            // arrange
            BrandModel brand1 = BrandFixture.createBrandWithLikeCount(new BigDecimal("1"));
            BrandModel brand2 = BrandFixture.createBrandWithLikeCount(new BigDecimal("10"));
            Long userId = 1L;

            // act - 동일한 서비스 인스턴스로 서로 다른 브랜드 처리
            brandLikeService.addLike(brand1, userId);
            brandLikeService.addLike(brand2, userId);

            // assert - 각각 독립적으로 처리됨
            assertAll(
                    () -> assertThat(brand1.getLikeCount().getBrandLikeCount()).isEqualByComparingTo(new BigDecimal("2")),
                    () -> assertThat(brand2.getLikeCount().getBrandLikeCount()).isEqualByComparingTo(new BigDecimal("11"))
            );
        }
    }
}
