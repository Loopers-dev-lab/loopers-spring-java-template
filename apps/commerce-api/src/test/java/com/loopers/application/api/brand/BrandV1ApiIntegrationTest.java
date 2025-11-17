package com.loopers.application.api.brand;

import com.loopers.application.api.ApiIntegrationTest;
import com.loopers.application.api.common.dto.ApiResponse;
import com.loopers.core.domain.brand.Brand;
import com.loopers.core.domain.brand.repository.BrandRepository;
import com.loopers.core.domain.brand.vo.BrandId;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static com.loopers.application.api.brand.BrandV1Dto.GetBrandResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.field;

class BrandV1ApiIntegrationTest extends ApiIntegrationTest {

    @Autowired
    BrandRepository brandRepository;

    @Nested
    @DisplayName("브랜드 조회")
    class 브랜드_조회 {

        @Nested
        @DisplayName("정상 요청인 경우")
        class 정상_요청인_경우 {

            String brandId;

            @BeforeEach
            void setUp() {
                brandId = brandRepository.save(
                        Instancio.of(Brand.class)
                                .set(field(Brand::getId), BrandId.empty())
                                .create()
                ).getId().value();
            }

            @Test
            @DisplayName("Status 200")
            void Status200() {
                //given
                String endPoint = "/api/v1/brands/" + brandId;
                ParameterizedTypeReference<ApiResponse<GetBrandResponse>> responseType =
                        new ParameterizedTypeReference<>() {
                        };

                //when
                ResponseEntity<ApiResponse<GetBrandResponse>> response =
                        testRestTemplate.exchange(endPoint, HttpMethod.GET, HttpEntity.EMPTY, responseType);

                //then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            }
        }

        @Nested
        @DisplayName("브랜드가 존재하지 않는 경우")
        class 브랜드가_존재하지_않는_경우 {

            @Test
            @DisplayName("Status 404")
            void Status404() {
                String endPoint = "/api/v1/brands/99999";
                ParameterizedTypeReference<ApiResponse<GetBrandResponse>> responseType =
                        new ParameterizedTypeReference<>() {
                        };
                ResponseEntity<ApiResponse<GetBrandResponse>> response =
                        testRestTemplate.exchange(endPoint, HttpMethod.GET, HttpEntity.EMPTY, responseType);

                //then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            }
        }
    }
}
