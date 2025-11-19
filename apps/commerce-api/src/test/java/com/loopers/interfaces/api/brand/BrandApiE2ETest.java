package com.loopers.interfaces.api.brand;

import com.loopers.domain.brand.Brand;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.brand.BrandDto.BrandViewResponse;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BrandApiE2ETest {

  private static final ParameterizedTypeReference<ApiResponse<BrandViewResponse>> BRAND_RESPONSE_TYPE = new ParameterizedTypeReference<>() {
  };

  private final TestRestTemplate testRestTemplate;
  private final BrandJpaRepository brandJpaRepository;
  private final DatabaseCleanUp databaseCleanUp;

  @Autowired
  public BrandApiE2ETest(
      TestRestTemplate testRestTemplate,
      BrandJpaRepository brandJpaRepository,
      DatabaseCleanUp databaseCleanUp)
   {
    this.testRestTemplate = testRestTemplate;
    this.brandJpaRepository = brandJpaRepository;
    this.databaseCleanUp = databaseCleanUp;
  }

  @AfterEach
  void tearDown() {
    databaseCleanUp.truncateAllTables();
  }

  @DisplayName("GET /api/v1/brands/{brandId}")
  @Nested
  class GetBrand {

    @DisplayName("브랜드가 존재하면 브랜드 정보를 반환한다")
    @Test
    void returnsBrandInfo_whenBrandExists() {
      Brand brand = Brand.of("나이키", "스포츠 브랜드");
      Brand savedBrand = brandJpaRepository.save(brand);

      ResponseEntity<ApiResponse<BrandViewResponse>> response =
          testRestTemplate.exchange(
              "/api/v1/brands/" + savedBrand.getId(),
              HttpMethod.GET,
              null,
              BRAND_RESPONSE_TYPE
          );

      assertAll(
          () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
          () -> assertThat(response.getBody()).isNotNull(),
          () -> assertThat(response.getBody().data())
              .extracting("brandId", "name", "description")
              .containsExactly(savedBrand.getId(), "나이키", "스포츠 브랜드")
      );
    }

    @DisplayName("브랜드가 존재하지 않으면 404를 반환한다")
    @Test
    void returnsNotFound_whenBrandDoesNotExist() {
      long nonExistentBrandId = 999L;

      ResponseEntity<ApiResponse<BrandViewResponse>> response =
          testRestTemplate.exchange(
              "/api/v1/brands/" + nonExistentBrandId,
              HttpMethod.GET,
              null,
              BRAND_RESPONSE_TYPE
          );

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
  }
}
