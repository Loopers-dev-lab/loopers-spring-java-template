package com.loopers.core.service.brand;

import com.loopers.core.domain.brand.Brand;
import com.loopers.core.domain.brand.BrandRepository;
import com.loopers.core.domain.brand.vo.BrandDescription;
import com.loopers.core.domain.brand.vo.BrandId;
import com.loopers.core.domain.brand.vo.BrandName;
import com.loopers.core.service.IntegrationTest;
import com.loopers.core.service.brand.query.GetBrandQuery;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class BrandQueryServiceTest extends IntegrationTest {

    @Autowired
    private BrandQueryService brandQueryService;

    @Autowired
    private BrandRepository brandRepository;

    @Nested
    @DisplayName("브랜드 조회 시")
    class 브랜드_조회 {

        @Nested
        @DisplayName("brandId의 브랜드가 존재하는 경우")
        class 브랜드가_존재하는_경우 {

            private BrandId brandId;

            @BeforeEach
            void setUp() {
                brandId = brandRepository.save(Brand.create(
                        new BrandName("loopers"),
                        new BrandDescription("education brand")
                )).getBrandId();
            }

            @Test
            @DisplayName("브랜드가 조회된다.")
            void 브랜드가_조회된다() {
                Brand find = brandQueryService.getBrandBy(
                        new GetBrandQuery(brandId.value())
                );

                Assertions.assertThat(find).isNotNull();
            }
        }
    }
}
