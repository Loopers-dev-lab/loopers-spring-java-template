package com.loopers.interfaces.api;

import com.loopers.domain.brand.Brand;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.interfaces.api.brand.BrandV1Dto;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BrandV1ApiE2ETest {

    private static final Function<Long, String> ENDPOINT_GET = id -> "/api/v1/brands/" + id;

    private final TestRestTemplate testRestTemplate;
    private final BrandJpaRepository brandJpaRepository;
    private final DatabaseCleanUp databaseCleanUp;

    @Autowired
    public BrandV1ApiE2ETest(
        TestRestTemplate testRestTemplate,
        BrandJpaRepository brandJpaRepository,
        DatabaseCleanUp databaseCleanUp
    ) {
        this.testRestTemplate = testRestTemplate;
        this.brandJpaRepository = brandJpaRepository;
        this.databaseCleanUp = databaseCleanUp;
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("GET /api/v1/brands/{id}")
    @Nested
    class Get {
        @DisplayName("존재하는 브랜드 ID가 주어지면, 브랜드 정보를 반환한다.")
        @Test
        void returnsBrandInfo_whenValidIdProvided() {
            // arrange
            Brand brand = brandJpaRepository.save(
                    Brand.create("나이키", "운동복 브랜드입니다.")
            );

            // act
            ParameterizedTypeReference<ApiResponse<BrandV1Dto.BrandResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<BrandV1Dto.BrandResponse>> response =
                testRestTemplate.exchange(ENDPOINT_GET.apply(brand.getId()), HttpMethod.GET, new HttpEntity<>(null), responseType);

            // assert
            assertThat(response.getBody()).isNotNull();
            assertAll(
                () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                () -> assertThat(response.getBody().data().id()).isEqualTo(brand.getId()),
                () -> assertThat(response.getBody().data().name()).isEqualTo(brand.getName()),
                () -> assertThat(response.getBody().data().description()).isEqualTo(brand.getDescription())
            );
        }

        @DisplayName("존재하지 않는 브랜드 ID가 주어지면, 404 Not Found 응답을 반환한다.")
        @Test
        void returnsNotFound_whenInvalidIdProvided() {
            // arrange
            Long invalidId = -1L;

            // act
            ParameterizedTypeReference<ApiResponse<BrandV1Dto.BrandResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<BrandV1Dto.BrandResponse>> response =
                testRestTemplate.exchange(ENDPOINT_GET.apply(invalidId), HttpMethod.GET, new HttpEntity<>(null), responseType);

            // assert
            assertAll(
                () -> assertTrue(response.getStatusCode().is4xxClientError()),
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
            );
        }
    }
}
