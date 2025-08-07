package com.loopers.domain.like;

import com.loopers.domain.like.brand.BrandLikeModel;
import com.loopers.domain.like.fixture.BrandLikeFixture;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BrandLikeModelTest {

    @Nested
    @DisplayName("브랜드 좋아요 생성 관련 테스트")
    class CreateTest {

        @DisplayName("정상적인 값으로 브랜드 좋아요를 생성할 수 있다")
        @Test
        void create_withValidValues() {
            // arrange
            Long userId = 1L;
            Long brandId = 1L;
            LocalDateTime beforeCreation = LocalDateTime.now();

            // act
            BrandLikeModel brandLike = BrandLikeModel.create(userId, brandId);

            // assert
            assertAll(
                    () -> assertThat(brandLike).isNotNull(),
                    () -> assertThat(brandLike.getUserId()).isEqualTo(userId),
                    () -> assertThat(brandLike.getBrandId()).isEqualTo(brandId),
                    () -> assertThat(brandLike.getLikedAt()).isAfter(beforeCreation)
            );
        }

        @DisplayName("사용자 ID가 null이면 생성에 실패한다")
        @Test
        void create_whenUserIdNull() {
            // arrange
            Long userId = null;
            Long brandId = 1L;
            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                BrandLikeModel.create(userId, brandId);
            });
            //assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("브랜드 ID가 null이면 생성에 실패한다")
        @Test
        void create_whenBrandIdNull() {
            // arrange
            Long userId = 1L;
            Long brandId = null;

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                BrandLikeModel.create(userId, brandId);
            });
            //assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);

        }

        @DisplayName("사용자 ID와 브랜드 ID가 모두 null이면 생성에 실패한다")
        @Test
        void create_whenBothIdsNull() {
            // arrange
            Long userId = null;
            Long brandId = null;
            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                BrandLikeModel.create(userId, brandId);
            });
            //assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("브랜드 좋아요 조회 관련 테스트")
    class QueryTest {

        @DisplayName("특정 사용자에게 속하는 좋아요인지 확인할 수 있다")
        @Test
        void belongsToUser_withCorrectUserId() {
            // arrange
            Long userId = 1L;
            Long brandId = 1L;
            BrandLikeModel brandLike = BrandLikeFixture.createBrandLikeModel(userId, brandId);

            // act & assert
            assertThat(brandLike.belongsToUser(userId)).isTrue();
            assertThat(brandLike.belongsToUser(2L)).isFalse();
        }

        @DisplayName("특정 브랜드에 대한 좋아요인지 확인할 수 있다")
        @Test
        void isForBrand_withCorrectBrandId() {
            // arrange
            Long userId = 1L;
            Long brandId = 1L;
            BrandLikeModel brandLike = BrandLikeFixture.createBrandLikeModel(userId, brandId);

            // act & assert
            assertThat(brandLike.isForBrand(brandId)).isTrue();
            assertThat(brandLike.isForBrand(2L)).isFalse();
        }

        @DisplayName("null 사용자 ID로 조회하면 false를 반환한다")
        @Test
        void belongsToUser_withNullUserId() {
            // arrange
            BrandLikeModel brandLike = BrandLikeFixture.createBrandLikeModel();

            // act & assert
            assertThat(brandLike.belongsToUser(null)).isFalse();
        }

        @DisplayName("null 브랜드 ID로 조회하면 false를 반환한다")
        @Test
        void isForBrand_withNullBrandId() {
            // arrange
            BrandLikeModel brandLike = BrandLikeFixture.createBrandLikeModel();

            // act & assert
            assertThat(brandLike.isForBrand(null)).isFalse();
        }
    }

    @Nested
    @DisplayName("브랜드 좋아요 비즈니스 로직 테스트")
    class BusinessLogicTest {

        @DisplayName("좋아요 생성 시 현재 시간이 설정된다")
        @Test
        void create_setsCurrentTime() {
            // arrange
            LocalDateTime beforeCreation = LocalDateTime.now();

            // act
            BrandLikeModel brandLike = BrandLikeFixture.createBrandLikeModel();
            LocalDateTime afterCreation = LocalDateTime.now();

            // assert
            assertAll(
                    () -> assertThat(brandLike.getLikedAt()).isAfter(beforeCreation.minusSeconds(1)),
                    () -> assertThat(brandLike.getLikedAt()).isBefore(afterCreation.plusSeconds(1))
            );
        }

        @DisplayName("동일한 사용자와 브랜드로 생성된 좋아요는 같은 값을 가진다")
        @Test
        void create_withSameParameters() {
            // arrange
            Long userId = 1L;
            Long brandId = 1L;

            // act
            BrandLikeModel like1 = BrandLikeModel.create(userId, brandId);
            BrandLikeModel like2 = BrandLikeModel.create(userId, brandId);

            // assert
            assertAll(
                    () -> assertThat(like1.getUserId()).isEqualTo(like2.getUserId()),
                    () -> assertThat(like1.getBrandId()).isEqualTo(like2.getBrandId()),
                    () -> assertThat(like1.belongsToUser(userId)).isTrue(),
                    () -> assertThat(like2.belongsToUser(userId)).isTrue(),
                    () -> assertThat(like1.isForBrand(brandId)).isTrue(),
                    () -> assertThat(like2.isForBrand(brandId)).isTrue()
            );
        }

        @DisplayName("remove 메서드가 정상적으로 호출된다")
        @Test
        void remove_callsSuccessfully() {
            // arrange
            BrandLikeModel brandLike = BrandLikeFixture.createBrandLikeModel();

            // act & assert - 예외가 발생하지 않음을 확인
            brandLike.remove();
            assertThat(brandLike).isNotNull();
        }
    }

    @Nested
    @DisplayName("Fixture를 사용한 테스트")
    class FixtureTest {

        @DisplayName("기본 Fixture로 생성할 수 있다")
        @Test
        void createWithDefaultFixture() {
            // act
            BrandLikeModel brandLike = BrandLikeFixture.createBrandLikeModel();

            // assert
            assertAll(
                    () -> assertThat(brandLike).isNotNull(),
                    () -> assertThat(brandLike.getUserId()).isEqualTo(BrandLikeFixture.DEFAULT_USER_ID),
                    () -> assertThat(brandLike.getBrandId()).isEqualTo(BrandLikeFixture.DEFAULT_BRAND_ID)
            );
        }

        @DisplayName("특정 사용자 ID로 Fixture를 생성할 수 있다")
        @Test
        void createWithSpecificUserId() {
            // arrange
            Long userId = 100L;

            // act
            BrandLikeModel brandLike = BrandLikeFixture.createBrandLikeWithUserId(userId);

            // assert
            assertAll(
                    () -> assertThat(brandLike.getUserId()).isEqualTo(userId),
                    () -> assertThat(brandLike.getBrandId()).isEqualTo(BrandLikeFixture.DEFAULT_BRAND_ID)
            );
        }

        @DisplayName("특정 브랜드 ID로 Fixture를 생성할 수 있다")
        @Test
        void createWithSpecificBrandId() {
            // arrange
            Long brandId = 200L;

            // act
            BrandLikeModel brandLike = BrandLikeFixture.createBrandLikeWithBrandId(brandId);

            // assert
            assertAll(
                    () -> assertThat(brandLike.getUserId()).isEqualTo(BrandLikeFixture.DEFAULT_USER_ID),
                    () -> assertThat(brandLike.getBrandId()).isEqualTo(brandId)
            );
        }
    }
}
