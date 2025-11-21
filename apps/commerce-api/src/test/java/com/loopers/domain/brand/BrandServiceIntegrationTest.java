package com.loopers.domain.brand;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.loopers.domain.brand.dto.BrandSearchFilter;
import com.loopers.fixtures.BrandTestFixture;
import com.loopers.support.error.CoreException;
import com.loopers.utils.DatabaseCleanUp;

@SpringBootTest
@DisplayName("BrandService 통합 테스트")
class BrandServiceIntegrationTest {
    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private BrandService brandService;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Nested
    @DisplayName("브랜드 목록 조회")
    class ListBrandsTest {
        @Test
        @DisplayName("브랜드명으로 검색하여 페이지네이션된 결과를 조회한다")
        void search_brands_by_name_with_pagination() {
            // given
            BrandTestFixture.saveBrands(brandRepository, 5);
            BrandTestFixture.createAndSave(brandRepository, "특별한브랜드", "설명");

            BrandSearchFilter filter = BrandSearchFilter.of("특별한");
            Pageable pageable = Pageable.ofSize(10).withPage(0);

            // when
            Page<BrandEntity> resultPages = brandService.searchBrands(filter, pageable);

            // then
            assertThat(resultPages.getContent()).hasSize(1);
            assertThat(resultPages.getContent().get(0).getName()).isEqualTo("특별한브랜드");
        }

        @Test
        @DisplayName("검색어가 없으면 전체 브랜드를 페이지네이션하여 조회한다")
        void search_brands_without_filter_returns_all() {
            // given
            BrandTestFixture.saveBrands(brandRepository, 5);
            Pageable pageable = Pageable.ofSize(10).withPage(0);

            // when
            Page<BrandEntity> resultPages = brandService.searchBrands(BrandSearchFilter.of(null), pageable);

            // then
            assertThat(resultPages.getTotalElements()).isEqualTo(5L);
        }
    }

    @Nested
    @DisplayName("브랜드 상세 조회")
    class GetBrandTest {
        @Test
        @DisplayName("브랜드 상세 정보를 조회하는데 성공한다")
        void get_brand_detail_success() {
            // given
            BrandEntity savedBrand = BrandTestFixture.saveBrands(brandRepository, 1).get(0);

            // when
            BrandEntity brand = brandService.getBrandById(savedBrand.getId());

            // then
            assertThat(brand).isNotNull();
            assertThat(brand.getId()).isEqualTo(savedBrand.getId());
            assertThat(brand.getName()).isEqualTo(savedBrand.getName());
            assertThat(brand.getCreatedAt()).isEqualTo(savedBrand.getCreatedAt());
            assertThat(brand.getDeletedAt()).isNull();
        }

        @Test
        @DisplayName("브랜드 이름으로 상세 정보를 조회하는데 성공한다")
        void get_brand_by_name_success() {
            // given
            BrandEntity savedBrand = BrandTestFixture.saveBrands(brandRepository, 1).get(0);

            // when
            BrandEntity brand = brandService.getBrandByName(savedBrand.getName());

            // then
            assertThat(brand).isNotNull();
            assertThat(brand.getName()).isEqualTo(savedBrand.getName());
            assertThat(brand.getId()).isEqualTo(savedBrand.getId());
        }

        @Test
        @DisplayName("존재하지 않는 브랜드 이름으로 조회하면 null을 반환한다")
        void get_brand_by_name_returns_null_when_not_found() {
            // given
            String nonExistentName = "존재하지않는브랜드";

            Assertions.assertThatThrownBy(() -> brandService.getBrandByName(nonExistentName)).isInstanceOf(CoreException.class).hasMessage("브랜드를 찾을 수 없습니다. (이름: 존재하지않는브랜드)");
        }

        @Test
        @DisplayName("브랜드의 id 기준 찾지 못하면 null을 반환한다")
        void get_brand_return_null_when_not_found() {
            // given
            BrandTestFixture.saveBrands(brandRepository, 5);
            long nonExistentId = 6L;

            // when
            Assertions.assertThatThrownBy(() -> brandService.getBrandById(nonExistentId)).isInstanceOf(CoreException.class).hasMessage("브랜드를 찾을 수 없습니다. (ID: 6)");

        }
    }
}
