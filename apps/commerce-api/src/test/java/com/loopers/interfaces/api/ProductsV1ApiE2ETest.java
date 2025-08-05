package com.loopers.interfaces.api;

import com.loopers.interfaces.api.product.ProductV1Dto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ProductsV1ApiE2ETest {
    private final TestRestTemplate testRestTemplate;

    @Autowired
    public ProductsV1ApiE2ETest(TestRestTemplate testRestTemplate) {
        this.testRestTemplate = testRestTemplate;
    }

    @Nested
    @DisplayName("GET /api/v1/products")
    class get {
        static String END_POINT = "/api/v1/products";
        @DisplayName("기본 조건으로 상품을 조회합니다.")
        @Test
        void getDroducts(){
            //arrange
            //act
            ParameterizedTypeReference<ApiResponse<ProductV1Dto.ListResponse>> responseType =
                    new ParameterizedTypeReference<ApiResponse<ProductV1Dto.ListResponse>>() {};
            ResponseEntity<ApiResponse<ProductV1Dto.ListResponse>> response =
                    testRestTemplate.exchange(END_POINT, HttpMethod.GET, new HttpEntity<>(null), responseType);
            //assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful())
            );
        }
        @DisplayName("brandId를 이용해서 상품을 조회합니다.")
        @Test
        void getDroductsBybrandId(){
            //arrange
            Long brandId = 3L;
            String urlWithParams = UriComponentsBuilder.fromPath(END_POINT)
                    .queryParam("brandId", brandId)
                    .toUriString();


            //act
            ParameterizedTypeReference<ApiResponse<ProductV1Dto.ListResponse>> responseType =
                    new ParameterizedTypeReference<ApiResponse<ProductV1Dto.ListResponse>>() {};
            ResponseEntity<ApiResponse<ProductV1Dto.ListResponse>> response =
                    testRestTemplate.exchange(urlWithParams, HttpMethod.GET, new HttpEntity<>(null), responseType);
            //assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody().data().items().get(0).brandId()).isEqualTo(brandId)
            );
        }
    }
}
