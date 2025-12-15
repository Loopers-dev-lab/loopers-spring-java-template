package com.loopers.interfaces.api.like;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.User;
import com.loopers.fixture.TestFixture;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LikeV1ApiE2ETest {

    private static final String ENDPOINT_LIKE = "/api/v1/like/products";

    private final TestRestTemplate testRestTemplate;
    private final DatabaseCleanUp databaseCleanUp;
    private final TestFixture testFixture;
    private final ProductRepository productRepository;

    @Autowired
    public LikeV1ApiE2ETest(
            TestRestTemplate testRestTemplate,
            DatabaseCleanUp databaseCleanUp,
            TestFixture testFixture,
            ProductRepository productRepository
    ) {
        this.testRestTemplate = testRestTemplate;
        this.databaseCleanUp = databaseCleanUp;
        this.testFixture = testFixture;
        this.productRepository = productRepository;
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("POST /api/v1/like/products/{productId}")
    @Nested
    class AddLike {

        @DisplayName("좋아요 등록에 성공할 경우, 200 OK 응답을 반환한다.")
        @Test
        void returnsOk_whenAddLikeIsSuccessful() {
            // arrange
            User user = testFixture.createUser("testuser01");
            Brand brand = testFixture.createBrand("Nike");
            Product product = testFixture.createProduct(brand);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", user.getLoginId());

            // act
            ParameterizedTypeReference<ApiResponse<Void>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Void>> response =
                    testRestTemplate.exchange(
                            ENDPOINT_LIKE + "/" + product.getId(),
                            HttpMethod.POST,
                            new HttpEntity<>(headers),
                            responseType
                    );

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK)
            );
        }

        @DisplayName("X-USER-ID 헤더가 없을 경우, 400 Bad Request 응답을 반환한다.")
        @Test
        void returnsBadRequest_whenUserIdHeaderIsMissing() {
            // arrange
            Brand brand = testFixture.createBrand("Nike");
            Product product = testFixture.createProduct(brand);

            // act
            ParameterizedTypeReference<ApiResponse<Void>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Void>> response =
                    testRestTemplate.exchange(
                            ENDPOINT_LIKE + "/" + product.getId(),
                            HttpMethod.POST,
                            null,
                            responseType
                    );

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
            );
        }

        @DisplayName("존재하지 않는 사용자로 좋아요 등록 시, 404 Not Found 응답을 반환한다.")
        @Test
        void returnsNotFound_whenUserDoesNotExist() {
            // arrange
            Brand brand = testFixture.createBrand("Nike");
            Product product = testFixture.createProduct(brand);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", "nonexistentuser");

            // act
            ParameterizedTypeReference<ApiResponse<Void>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Void>> response =
                    testRestTemplate.exchange(
                            ENDPOINT_LIKE + "/" + product.getId(),
                            HttpMethod.POST,
                            new HttpEntity<>(headers),
                            responseType
                    );

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
            );
        }

        @DisplayName("존재하지 않는 상품에 좋아요 등록 시, 404 Not Found 응답을 반환한다.")
        @Test
        void returnsNotFound_whenProductDoesNotExist() {
            // arrange
            User user = testFixture.createUser("testuser01");

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", user.getLoginId());

            // act
            ParameterizedTypeReference<ApiResponse<Void>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Void>> response =
                    testRestTemplate.exchange(
                            ENDPOINT_LIKE + "/99999",
                            HttpMethod.POST,
                            new HttpEntity<>(headers),
                            responseType
                    );

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
            );
        }

        @DisplayName("동일 상품에 중복 좋아요 등록 시에도, 200 OK 응답을 반환한다. (멱등성)")
        @Test
        void returnsOk_whenAddLikeDuplicate() {
            // arrange
            User user = testFixture.createUser("testuser01");
            Brand brand = testFixture.createBrand("Nike");
            Product product = testFixture.createProduct(brand);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", user.getLoginId());

            // 첫 번째 좋아요
            testRestTemplate.exchange(
                    ENDPOINT_LIKE + "/" + product.getId(),
                    HttpMethod.POST,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<ApiResponse<Void>>() {}
            );

            // act - 두 번째 좋아요 (중복)
            ParameterizedTypeReference<ApiResponse<Void>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Void>> response =
                    testRestTemplate.exchange(
                            ENDPOINT_LIKE + "/" + product.getId(),
                            HttpMethod.POST,
                            new HttpEntity<>(headers),
                            responseType
                    );

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK)
            );
        }
    }

    @DisplayName("DELETE /api/v1/like/products/{productId}")
    @Nested
    class RemoveLike {

        @DisplayName("좋아요 취소에 성공할 경우, 200 OK 응답을 반환한다.")
        @Test
        void returnsOk_whenRemoveLikeIsSuccessful() {
            // arrange
            User user = testFixture.createUser("testuser01");
            Brand brand = testFixture.createBrand("Nike");
            Product product = testFixture.createProduct(brand);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", user.getLoginId());

            // 좋아요 등록
            testRestTemplate.exchange(
                    ENDPOINT_LIKE + "/" + product.getId(),
                    HttpMethod.POST,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<ApiResponse<Void>>() {}
            );

            // act - 좋아요 취소
            ParameterizedTypeReference<ApiResponse<Void>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Void>> response =
                    testRestTemplate.exchange(
                            ENDPOINT_LIKE + "/" + product.getId(),
                            HttpMethod.DELETE,
                            new HttpEntity<>(headers),
                            responseType
                    );

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK)
            );
        }

        @DisplayName("좋아요 하지 않은 상품에 취소 요청해도, 200 OK 응답을 반환한다. (멱등성)")
        @Test
        void returnsOk_whenRemoveLikeNotExist() {
            // arrange
            User user = testFixture.createUser("testuser01");
            Brand brand = testFixture.createBrand("Nike");
            Product product = testFixture.createProduct(brand);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", user.getLoginId());

            // act - 좋아요 없이 취소 요청
            ParameterizedTypeReference<ApiResponse<Void>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Void>> response =
                    testRestTemplate.exchange(
                            ENDPOINT_LIKE + "/" + product.getId(),
                            HttpMethod.DELETE,
                            new HttpEntity<>(headers),
                            responseType
                    );

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK)
            );
        }

        @DisplayName("X-USER-ID 헤더가 없을 경우, 400 Bad Request 응답을 반환한다.")
        @Test
        void returnsBadRequest_whenUserIdHeaderIsMissing() {
            // arrange
            Brand brand = testFixture.createBrand("Nike");
            Product product = testFixture.createProduct(brand);

            // act
            ParameterizedTypeReference<ApiResponse<Void>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Void>> response =
                    testRestTemplate.exchange(
                            ENDPOINT_LIKE + "/" + product.getId(),
                            HttpMethod.DELETE,
                            null,
                            responseType
                    );

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
            );
        }

        @DisplayName("존재하지 않는 사용자로 좋아요 취소 시, 404 Not Found 응답을 반환한다.")
        @Test
        void returnsNotFound_whenUserDoesNotExist() {
            // arrange
            Brand brand = testFixture.createBrand("Nike");
            Product product = testFixture.createProduct(brand);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", "nonexistentuser");

            // act
            ParameterizedTypeReference<ApiResponse<Void>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Void>> response =
                    testRestTemplate.exchange(
                            ENDPOINT_LIKE + "/" + product.getId(),
                            HttpMethod.DELETE,
                            new HttpEntity<>(headers),
                            responseType
                    );

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
            );
        }
    }
}
