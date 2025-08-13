package com.loopers.domain.brand;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BrandTest {

    @Nested
    @DisplayName("브랜드 등록 관련 테스트")
    class RegisterTest {

        @DisplayName("정상적인 값으로 브랜드를 등록할 수 있다")
        @Test
        void register_withValidValues() {
            // arrange
            
            // act
            BrandModel brand = BrandFixture.createBrandModel();
            
            // assert
            assertThat(brand).isNotNull();
        }

        @DisplayName("브랜드명이 null이면 등록에 실패한다")
        @Test
        void register_whenBrandNameNull() {
            // arrange
            String brandName = null;

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                BrandFixture.createBrandWithName(brandName);
            });
            
            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("브랜드명에 특수문자가 포함되면 등록에 실패한다")
        @Test
        void register_whenBrandNameContainsSpecialCharacters() {
            // arrange
            String brandName = "Brand@#$";

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                BrandFixture.createBrandWithName(brandName);
            });
            
            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("SNS 링크가 유효하지 않은 URL이면 등록에 실패한다")
        @Test
        void register_whenInvalidSnsLink() {
            // arrange
            String invalidSnsLink = "invalid-url";

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                BrandFixture.createBrandWithSnsLink(invalidSnsLink);
            });
            
            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("좋아요 수가 음수이면 등록에 실패한다")
        @Test
        void register_whenLikeCountNegative() {
            // arrange
            BigDecimal likeCount = new BigDecimal("-1");

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                BrandFixture.createBrandWithLikeCount(likeCount);
            });
            
            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("비활성 상태로 브랜드를 등록할 수 있다")
        @Test
        void register_withInactiveStatus() {
            // arrange
            
            // act
            BrandModel brand = BrandFixture.createInactiveBrand();
            
            // assert
            assertThat(brand).isNotNull();
        }
    }

    @Nested
    @DisplayName("좋아요 관련 테스트")
    class LikeCountTest {

        @DisplayName("좋아요 수를 증가시킬 수 있다")
        @Test
        void incrementLikeCount() {
            // arrange
            BrandModel brand = BrandFixture.createBrandWithLikeCount(new BigDecimal("5"));

            // act
            brand.incrementLikeCount();

            // assert
            assertAll(
                    () -> assertThat(brand).isNotNull(),
                    () -> assertThat(brand.getLikeCount().getBrandLikeCount()).isEqualByComparingTo(new BigDecimal("6"))
            );
        }

        @DisplayName("좋아요 수를 감소시킬 수 있다")
        @Test
        void decrementLikeCount() {
            // arrange
            BrandModel brand = BrandFixture.createBrandWithLikeCount(new BigDecimal("5"));

            // act
            brand.decrementLikeCount();

            // assert
            assertAll(
                    () -> assertThat(brand).isNotNull(),
                    () -> assertThat(brand.getLikeCount().getBrandLikeCount()).isEqualByComparingTo(new BigDecimal("4"))
            );
        }

        @DisplayName("좋아요가 0일 때 감소시켜도 예외가 발생하지 않는다")
        @Test
        void decrementLikeCount_whenZero() {
            // arrange
            BrandModel brand = BrandFixture.createBrandWithLikeCount(BigDecimal.ZERO);

            // act
            brand.decrementLikeCount();

            // assert
            assertThat(brand).isNotNull();
        }
    }

    @Nested
    @DisplayName("브랜드 상태 관련 테스트")
    class BrandStatusTest {

        @DisplayName("활성 상태 브랜드를 생성할 수 있다")
        @Test
        void createActiveBrand() {
            // arrange
            
            // act
            BrandModel brand = BrandFixture.createBrandModel();

            // assert
            assertThat(brand).isNotNull();
        }

        @DisplayName("비활성 상태 브랜드를 생성할 수 있다")
        @Test
        void createInactiveBrand() {
            // arrange
            
            // act
            BrandModel brand = BrandFixture.createInactiveBrand();

            // assert
            assertThat(brand).isNotNull();
        }
    }

    @Nested
    @DisplayName("브랜드 유효성 검증 테스트")
    class ValidationTest {

        @DisplayName("유효한 브랜드명으로 생성할 수 있다")
        @Test
        void createBrand_withValidName() {
            // arrange
            String validName = "Valid Brand Name 123";

            // act
            BrandModel brand = BrandFixture.createBrandWithName(validName);

            // assert
            assertAll(
                    () -> assertThat(brand).isNotNull(),
                    () -> assertThat(brand.getBrandName().getValue()).isEqualTo(validName)
            );
        }

        @DisplayName("유효한 SNS 링크로 생성할 수 있다")
        @Test
        void createBrand_withValidSnsLink() {
            // arrange
            String validSnsLink = "https://instagram.com/validbrand";

            // act
            BrandModel brand = BrandFixture.createBrandWithSnsLink(validSnsLink);

            // assert
            assertThat(brand).isNotNull();
        }

        @DisplayName("좋아요 수가 0일 때 정상 생성된다")
        @Test
        void createBrand_withZeroLikeCount() {
            // arrange
            BigDecimal zeroLikeCount = BigDecimal.ZERO;

            // act
            BrandModel brand = BrandFixture.createBrandWithLikeCount(zeroLikeCount);

            // assert
            assertAll(
                    () -> assertThat(brand).isNotNull(),
                    () -> assertThat(brand.getLikeCount().getBrandLikeCount()).isEqualByComparingTo(BigDecimal.ZERO)
            );
        }
    }
}
