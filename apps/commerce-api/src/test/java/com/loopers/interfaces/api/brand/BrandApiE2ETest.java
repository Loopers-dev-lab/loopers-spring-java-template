package com.loopers.interfaces.api.brand;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
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
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BrandApiE2ETest {

    private static final String ENDPOINT = "/api/v1/brands";

    private final TestRestTemplate testRestTemplate;
    private final DatabaseCleanUp databaseCleanUp;

    private final BrandJpaRepository brandJpaRepository;
    private final ProductJpaRepository productJpaRepository;

    @Autowired
    public BrandApiE2ETest(
            TestRestTemplate testRestTemplate,
            DatabaseCleanUp databaseCleanUp,
            BrandJpaRepository brandJpaRepository,
            ProductJpaRepository productJpaRepository
    ) {
        this.testRestTemplate = testRestTemplate;
        this.databaseCleanUp = databaseCleanUp;
        this.brandJpaRepository = brandJpaRepository;
        this.productJpaRepository = productJpaRepository;
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("GET /api/v1/brands/{brandId}")
    @Nested
    class GetBrand {

        @DisplayName("브랜드 ID로 조회할 경우, 브랜드 정보와 상품 목록을 응답으로 반환한다.")
        @Test
        void brandTest1() {
            // arrange
            Brand brandA = brandJpaRepository.save(
                    Brand.create("브랜드A")
            );

            productJpaRepository.save(
                    Product.create("상품A", "설명A", 10_000, 100L, brandA.getId())
            );

            productJpaRepository.save(
                    Product.create("상품B", "설명B", 20_000, 50L, brandA.getId())
            );

            String url = ENDPOINT + "/" + brandA.getId();

            // act
            ParameterizedTypeReference<ApiResponse<BrandDto.BrandDetailResponse>> type =
                    new ParameterizedTypeReference<>() {
                    };

            ResponseEntity<ApiResponse<BrandDto.BrandDetailResponse>> response =
                    testRestTemplate.exchange(url, HttpMethod.GET, null, type);

            // assert
            assertAll(
                    () -> assertThat(response.getStatusCode().is2xxSuccessful()).isTrue(),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data().id()).isEqualTo(brandA.getId()),
                    () -> assertThat(response.getBody().data().name()).isEqualTo("브랜드A"),
                    () -> assertThat(response.getBody().data().products()).hasSize(2),
                    () -> assertThat(response.getBody().data().products().get(1).name()).isEqualTo("상품B")
            );
        }
    }

}
