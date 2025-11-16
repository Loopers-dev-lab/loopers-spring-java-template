package com.loopers.application.api.brand;

import com.loopers.application.api.ApiIntegrationTest;
import com.loopers.application.api.common.dto.ApiResponse;
import com.loopers.core.domain.brand.Brand;
import com.loopers.core.domain.brand.repository.BrandRepository;
import com.loopers.core.domain.brand.vo.BrandDescription;
import com.loopers.core.domain.brand.vo.BrandName;
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

import static org.assertj.core.api.Assertions.assertThat;

class BrandV1ApiTest extends ApiIntegrationTest {

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
                        Brand.create(new BrandName("kilian"), new BrandDescription("향수 브랜드"))
                ).getId().value();
            }

            @Test
            @DisplayName("브랜드의 정보를 조회한다.")
            void 브랜드의_정보를_조회한다() {
                //given
                String endPoint = "/api/v1/brands/" + brandId;
                ParameterizedTypeReference<ApiResponse<BrandV1Dto.GetBrandResponse>> responseType =
                        new ParameterizedTypeReference<>() {
                        };

                //when
                ResponseEntity<ApiResponse<BrandV1Dto.GetBrandResponse>> response =
                        testRestTemplate.exchange(endPoint, HttpMethod.GET, HttpEntity.EMPTY, responseType);

                //then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            }
        }
    }
}
