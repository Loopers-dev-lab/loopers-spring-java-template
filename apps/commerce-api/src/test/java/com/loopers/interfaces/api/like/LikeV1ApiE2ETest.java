package com.loopers.interfaces.api.like;

import com.loopers.application.members.MemberFacade;
import com.loopers.domain.members.enums.Gender;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.repository.BrandRepository;
import com.loopers.domain.common.vo.Money;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.vo.Stock;
import com.loopers.domain.product.repository.ProductRepository;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LikeV1ApiE2ETest {

    private final TestRestTemplate testRestTemplate;
    private final MemberFacade memberFacade;
    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final DatabaseCleanUp databaseCleanUp;

    @Autowired
    public LikeV1ApiE2ETest(
            TestRestTemplate testRestTemplate,
            MemberFacade memberFacade,
            BrandRepository brandRepository,
            ProductRepository productRepository,
            DatabaseCleanUp databaseCleanUp
    ) {
        this.testRestTemplate = testRestTemplate;
        this.memberFacade = memberFacade;
        this.brandRepository = brandRepository;
        this.productRepository = productRepository;
        this.databaseCleanUp = databaseCleanUp;
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    private Long setupProductAndMember() {
        // 회원 생성
        memberFacade.registerMember("test123", "test@example.com", "password", "1990-01-01", Gender.MALE);
        
        // 브랜드 생성
        Brand brand = brandRepository.save(new Brand("TestBrand", "Test Brand Description"));
        
        // 상품 생성
        Product product = new Product(
                brand.getId(),
                "Test Product",
                "Test Description",
                Money.of(BigDecimal.valueOf(10000)),
                Stock.of(100)
        );
        return productRepository.save(product).getId();
    }

    @DisplayName("상품 좋아요 (POST /api/v1/products/{productId}/likes)")
    @Nested
    class LikeProduct {

        @DisplayName("유효한 요청으로 좋아요 시 200을 반환한다")
        @Test
        void shouldReturn200_whenValidRequest() {
            Long productId = setupProductAndMember();

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", "test123");

            ParameterizedTypeReference<ApiResponse<Void>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Void>> response =
                    testRestTemplate.exchange("/api/v1/products/" + productId + "/likes", 
                            HttpMethod.POST, new HttpEntity<>(null, headers), responseType);

            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful())
            );
        }

        @DisplayName("존재하지 않는 상품에 좋아요 시 404를 반환한다")
        @Test
        void shouldReturn404_whenProductNotFound() {
            memberFacade.registerMember("test123", "test@example.com", "password", "1990-01-01", Gender.MALE);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", "test123");

            ParameterizedTypeReference<ApiResponse<Void>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Void>> response =
                    testRestTemplate.exchange("/api/v1/products/999/likes", 
                            HttpMethod.POST, new HttpEntity<>(null, headers), responseType);

            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
            );
        }
    }

    @DisplayName("상품 좋아요 취소 (DELETE /api/v1/products/{productId}/likes)")
    @Nested
    class UnlikeProduct {

        @DisplayName("유효한 요청으로 좋아요 취소 시 200을 반환한다")
        @Test
        void shouldReturn200_whenValidRequest() {
            Long productId = setupProductAndMember();

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", "test123");

            // 먼저 좋아요
            testRestTemplate.exchange("/api/v1/products/" + productId + "/likes", 
                    HttpMethod.POST, new HttpEntity<>(null, headers), 
                    new ParameterizedTypeReference<ApiResponse<Void>>() {});

            // 좋아요 취소
            ParameterizedTypeReference<ApiResponse<Void>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Void>> response =
                    testRestTemplate.exchange("/api/v1/products/" + productId + "/likes", 
                            HttpMethod.DELETE, new HttpEntity<>(null, headers), responseType);

            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful())
            );
        }
    }
}