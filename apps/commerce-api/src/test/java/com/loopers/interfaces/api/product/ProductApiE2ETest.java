package com.loopers.interfaces.api.product;

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
class ProductApiE2ETest {
    private static final String ENDPOINT = "/api/v1/products";

    private final TestRestTemplate testRestTemplate;
    private final DatabaseCleanUp databaseCleanUp;


    @Autowired
    public ProductApiE2ETest(
            TestRestTemplate testRestTemplate,
            DatabaseCleanUp databaseCleanUp
    ) {
        this.testRestTemplate = testRestTemplate;
        this.databaseCleanUp = databaseCleanUp;
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("GET /api/v1/products")
    @Nested
    class GetProducts {

        @DisplayName("조건에 맞는 상품이 없을 경우, 빈 배열을 응답으로 반환한다.")
        @Test
        void productTest1() {
            // arrange
            String url = ENDPOINT + "?page=0&size=20";

            // act
            ParameterizedTypeReference<ApiResponse<ProductDto.ProductListResponse>> type =
                    new ParameterizedTypeReference<>() {
                    };

            ResponseEntity<ApiResponse<ProductDto.ProductListResponse>> response =
                    testRestTemplate.exchange(url, HttpMethod.GET, null, type);

            // assert
            assertAll(
                    () -> assertThat(response.getStatusCode().is2xxSuccessful()).isTrue(),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data()).isNotNull(),
                    () -> assertThat(response.getBody().data().products()).isEmpty(),
                    () -> assertThat(response.getBody().data().totalCount()).isEqualTo(0)
            );
        }
    }
}
