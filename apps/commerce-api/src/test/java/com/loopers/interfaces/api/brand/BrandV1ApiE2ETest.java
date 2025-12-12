package com.loopers.interfaces.api.brand;

import com.loopers.domain.brand.Brand;
import com.loopers.fixture.TestFixture;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BrandV1ApiE2ETest {

    private static final String ENDPOINT_GET_BRAND = "/api/v1/brands";

    private final TestRestTemplate testRestTemplate;
    private final DatabaseCleanUp databaseCleanUp;
    private final TestFixture testFixture;

    @Autowired
    public BrandV1ApiE2ETest(
            TestRestTemplate testRestTemplate,
            DatabaseCleanUp databaseCleanUp,
            TestFixture testFixture
    ) {
        this.testRestTemplate = testRestTemplate;
        this.databaseCleanUp = databaseCleanUp;
        this.testFixture = testFixture;
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("GET /api/v1/brands/{brandId}")
    @Nested
    class GetBrand {

        @DisplayName("브랜드 조회에 성공할 경우, 브랜드 정보를 응답으로 반환한다.")
        @Test
        void returnsBrandInfo_whenBrandExists() {
            // arrange
            Brand brand = testFixture.createBrand("Nike");

            // act
            ParameterizedTypeReference<ApiResponse<BrandV1Dto.BrandResponse>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<BrandV1Dto.BrandResponse>> response =
                    testRestTemplate.exchange(
                            ENDPOINT_GET_BRAND + "/" + brand.getId(),
                            HttpMethod.GET,
                            null,
                            responseType
                    );

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data()).isNotNull(),
                    () -> assertThat(response.getBody().data().id()).isEqualTo(brand.getId()),
                    () -> assertThat(response.getBody().data().name()).isEqualTo("Nike")
            );
        }

        @DisplayName("존재하지 않는 브랜드 ID로 조회할 경우, 404 Not Found 응답을 반환한다.")
        @Test
        void returnsNotFound_whenBrandDoesNotExist() {
            // act
            ParameterizedTypeReference<ApiResponse<BrandV1Dto.BrandResponse>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<BrandV1Dto.BrandResponse>> response =
                    testRestTemplate.exchange(
                            ENDPOINT_GET_BRAND + "/99999",
                            HttpMethod.GET,
                            null,
                            responseType
                    );

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
            );
        }

        @DisplayName("여러 브랜드 중 특정 브랜드를 정확하게 조회한다.")
        @Test
        void returnsCorrectBrand_whenMultipleBrandsExist() {
            // arrange
            Brand nike = testFixture.createBrand("Nike");
            Brand adidas = testFixture.createBrand("Adidas");
            Brand stussy = testFixture.createBrand("Stussy");

            // act
            ParameterizedTypeReference<ApiResponse<BrandV1Dto.BrandResponse>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<BrandV1Dto.BrandResponse>> response =
                    testRestTemplate.exchange(
                            ENDPOINT_GET_BRAND + "/" + adidas.getId(),
                            HttpMethod.GET,
                            null,
                            responseType
                    );

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody().data().name()).isEqualTo("Adidas")
            );
        }
    }
}
