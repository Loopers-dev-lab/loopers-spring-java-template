package com.loopers.domain.brand;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Brand 도메인 테스트")
class BrandTest {

    private Brand createTestBrand(String name, BrandStatus status) {
        return Brand.reconstitute(
                1L,
                name,
                "테스트 브랜드 설명",
                "https://example.com/logo.jpg",
                status,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Nested
    @DisplayName("브랜드 조회")
    class BrandQueryTest {

        @Test
        @DisplayName("브랜드 상세 정보를 조회할 수 있다")
        void getBrandDetail() {
            // given
            Brand brand = createTestBrand("테스트 브랜드", BrandStatus.ACTIVE);

            // then
            assertThat(brand.getId()).isEqualTo(1L);
            assertThat(brand.getName()).isEqualTo("테스트 브랜드");
            assertThat(brand.getDescription()).isEqualTo("테스트 브랜드 설명");
            assertThat(brand.getImageUrl()).isEqualTo("https://example.com/logo.jpg");
            assertThat(brand.getStatus()).isEqualTo(BrandStatus.ACTIVE);
            assertThat(brand.getCreatedAt()).isNotNull();
            assertThat(brand.getModifiedAt()).isNotNull();
        }

        @Test
        @DisplayName("정상 브랜드는 조회 가능하다")
        void isViewable_activeBrand() {
            // given
            Brand brand = createTestBrand("테스트 브랜드", BrandStatus.ACTIVE);

            // then
            assertThat(brand.isViewable()).isTrue();
            assertThat(brand.isDeleted()).isFalse();
        }

        @Test
        @DisplayName("중단된 브랜드도 조회 가능하다")
        void isViewable_suspendedBrand() {
            // given
            Brand brand = createTestBrand("테스트 브랜드", BrandStatus.SUSPENDED);

            // then
            assertThat(brand.isViewable()).isTrue();
            assertThat(brand.isDeleted()).isFalse();
        }

        @Test
        @DisplayName("삭제된 브랜드는 조회 불가능하다")
        void isViewable_deletedBrand() {
            // given
            Brand brand = createTestBrand("테스트 브랜드", BrandStatus.DELETED);

            // then
            assertThat(brand.isViewable()).isFalse();
            assertThat(brand.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("경계값: 빈 설명을 가진 브랜드를 조회할 수 있다")
        void getBrand_withEmptyDescription() {
            // given
            Brand brand = Brand.reconstitute(
                    1L,
                    "테스트 브랜드",
                    "",
                    "logo.jpg",
                    BrandStatus.ACTIVE,
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );

            // then
            assertThat(brand.getDescription()).isEmpty();
            assertThat(brand.isViewable()).isTrue();
        }

        @Test
        @DisplayName("경계값: 매우 긴 이름을 가진 브랜드를 조회할 수 있다")
        void getBrand_withVeryLongName() {
            // given
            String longName = "A".repeat(255);
            Brand brand = Brand.reconstitute(
                    1L,
                    longName,
                    "설명",
                    "logo.jpg",
                    BrandStatus.ACTIVE,
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );

            // then
            assertThat(brand.getName()).hasSize(255);
            assertThat(brand.isViewable()).isTrue();
        }
    }

    @Nested
    @DisplayName("브랜드 상태 확인")
    class BrandStatusTest {

        @Test
        @DisplayName("ACTIVE 상태 브랜드는 활성 상태다")
        void isActive_activeBrand() {
            // given
            Brand brand = createTestBrand("테스트 브랜드", BrandStatus.ACTIVE);

            // then
            assertThat(brand.isActive()).isTrue();
            assertThat(brand.isSuspended()).isFalse();
            assertThat(brand.isDeleted()).isFalse();
        }

        @Test
        @DisplayName("SUSPENDED 상태 브랜드는 중단 상태다")
        void isSuspended_suspendedBrand() {
            // given
            Brand brand = createTestBrand("테스트 브랜드", BrandStatus.SUSPENDED);

            // then
            assertThat(brand.isSuspended()).isTrue();
            assertThat(brand.isActive()).isFalse();
            assertThat(brand.isDeleted()).isFalse();
        }

        @Test
        @DisplayName("DELETED 상태 브랜드는 삭제 상태다")
        void isDeleted_deletedBrand() {
            // given
            Brand brand = createTestBrand("테스트 브랜드", BrandStatus.DELETED);

            // then
            assertThat(brand.isDeleted()).isTrue();
            assertThat(brand.isActive()).isFalse();
            assertThat(brand.isSuspended()).isFalse();
        }

        @Test
        @DisplayName("활성 브랜드만 필터링할 수 있다")
        void filterActiveBrands() {
            // given
            Brand activeBrand = createTestBrand("활성 브랜드", BrandStatus.ACTIVE);
            Brand suspendedBrand = createTestBrand("중단 브랜드", BrandStatus.SUSPENDED);
            Brand deletedBrand = createTestBrand("삭제 브랜드", BrandStatus.DELETED);

            // then
            assertThat(activeBrand.isActive()).isTrue();
            assertThat(suspendedBrand.isActive()).isFalse();
            assertThat(deletedBrand.isActive()).isFalse();
        }
    }
}
