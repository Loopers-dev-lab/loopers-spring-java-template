package com.loopers.core.service.brand;

import com.loopers.core.domain.brand.Brand;
import com.loopers.core.domain.brand.repository.BrandRepository;
import com.loopers.core.domain.brand.vo.BrandDescription;
import com.loopers.core.domain.brand.vo.BrandId;
import com.loopers.core.domain.brand.vo.BrandName;
import com.loopers.core.domain.error.NotFoundException;
import com.loopers.core.service.IntegrationTest;
import com.loopers.core.service.brand.query.GetBrandQuery;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.instancio.Select.field;

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
                brandId = brandRepository.save(
                        Instancio.of(Brand.class)
                                .set(field(Brand::getId), BrandId.empty())
                                .set(field(Brand::getName), new BrandName("loopers"))
                                .set(field(Brand::getDescription), new BrandDescription("education brand"))
                                .create()
                ).getId();
            }

            @Test
            @DisplayName("브랜드가 조회된다.")
            void 브랜드가_조회된다() {
                Brand find = brandQueryService.getBrandBy(
                        new GetBrandQuery(brandId.value())
                );

                assertThat(find).isNotNull();
            }
        }

        @Nested
        @DisplayName("브랜드가 존재하지 않는 경우")
        class 브랜드가_존재하지_않는_경우 {

            @Test
            @DisplayName("예외가 발생한다.")
            void 예외가_발생한다() {
                assertThatThrownBy(() -> brandQueryService.getBrandBy(new GetBrandQuery("0")))
                        .isInstanceOf(NotFoundException.class);
            }
        }
    }
}
