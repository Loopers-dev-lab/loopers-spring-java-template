package com.loopers.interfaces.api.ranking;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.repository.BrandRepository;
import com.loopers.domain.common.vo.Money;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.repository.ProductRepository;
import com.loopers.domain.product.vo.Stock;
import com.loopers.infrastructure.cache.CacheKeyGenerator;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.testcontainers.RedisTestContainersConfig;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(RedisTestContainersConfig.class)
@DisplayName("랭킹 API E2E 테스트")
class RankingV1ApiE2ETest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private String todayDate;
    private Brand testBrand;

    @BeforeEach
    void setUp() {
        todayDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // Redis 초기화
        redisTemplate.getConnectionFactory().getConnection().flushDb();

        // 테스트 브랜드 생성
        testBrand = brandRepository.save(new Brand("TestBrand", "Test Brand Description"));
    }

    @AfterEach
    void tearDown() {
        redisTemplate.getConnectionFactory().getConnection().flushDb();
        databaseCleanUp.truncateAllTables();
    }

    private Product createProduct(String name, int price) {
        Product product = new Product(
                testBrand.getId(),
                name,
                "Description",
                Money.of(BigDecimal.valueOf(price)),
                Stock.of(100)
        );
        return productRepository.save(product);
    }

    private void addToRanking(Long productId, double score) {
        String key = CacheKeyGenerator.dailyRankingKey(todayDate);
        redisTemplate.opsForZSet().add(key, productId.toString(), score);
    }

    @DisplayName("랭킹 조회 (GET /api/v1/rankings)")
    @Nested
    class GetRankings {

        @DisplayName("랭킹 데이터가 있을 때 정상 응답을 반환한다")
        @Test
        void shouldReturnRankings_whenDataExists() {
            // given
            Product product1 = createProduct("인기상품1", 10000);
            Product product2 = createProduct("인기상품2", 20000);
            Product product3 = createProduct("인기상품3", 30000);

            addToRanking(product1.getId(), 100.0);
            addToRanking(product2.getId(), 80.0);
            addToRanking(product3.getId(), 60.0);

            // when
            ParameterizedTypeReference<ApiResponse<RankingV1Dto.RankingPageResponse>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<RankingV1Dto.RankingPageResponse>> response =
                    testRestTemplate.exchange("/api/v1/rankings", HttpMethod.GET, null, responseType);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data()).isNotNull(),
                    () -> assertThat(response.getBody().data().rankings()).hasSize(3),
                    () -> assertThat(response.getBody().data().date()).isEqualTo(todayDate),
                    // 1위는 점수가 가장 높은 product1
                    () -> assertThat(response.getBody().data().rankings().get(0).rank()).isEqualTo(1),
                    () -> assertThat(response.getBody().data().rankings().get(0).productId()).isEqualTo(product1.getId()),
                    () -> assertThat(response.getBody().data().rankings().get(0).productName()).isEqualTo("인기상품1"),
                    () -> assertThat(response.getBody().data().rankings().get(0).brandName()).isEqualTo("TestBrand"),
                    () -> assertThat(response.getBody().data().rankings().get(0).score()).isEqualTo(100.0),
                    // 2위
                    () -> assertThat(response.getBody().data().rankings().get(1).rank()).isEqualTo(2),
                    () -> assertThat(response.getBody().data().rankings().get(1).productId()).isEqualTo(product2.getId()),
                    // 3위
                    () -> assertThat(response.getBody().data().rankings().get(2).rank()).isEqualTo(3),
                    () -> assertThat(response.getBody().data().rankings().get(2).productId()).isEqualTo(product3.getId())
            );
        }

        @DisplayName("page 파라미터가 1-based로 동작한다")
        @Test
        void shouldWork_with1BasedPageParameter() {
            // given - 25개 상품 생성 및 랭킹 추가
            for (int i = 1; i <= 25; i++) {
                Product product = createProduct("상품" + i, 10000 * i);
                addToRanking(product.getId(), 100.0 - i);  // 상품1이 1위
            }

            // when - page=1 (첫 페이지)
            ParameterizedTypeReference<ApiResponse<RankingV1Dto.RankingPageResponse>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<RankingV1Dto.RankingPageResponse>> response =
                    testRestTemplate.exchange("/api/v1/rankings?page=1&size=10", HttpMethod.GET, null, responseType);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody().data().rankings()).hasSize(10),
                    () -> assertThat(response.getBody().data().page()).isEqualTo(1),
                    () -> assertThat(response.getBody().data().totalCount()).isEqualTo(25),
                    () -> assertThat(response.getBody().data().totalPages()).isEqualTo(3),
                    () -> assertThat(response.getBody().data().hasNext()).isTrue(),
                    // 첫 페이지는 1위~10위
                    () -> assertThat(response.getBody().data().rankings().get(0).rank()).isEqualTo(1),
                    () -> assertThat(response.getBody().data().rankings().get(9).rank()).isEqualTo(10)
            );

            // when - page=2 (두 번째 페이지)
            ResponseEntity<ApiResponse<RankingV1Dto.RankingPageResponse>> response2 =
                    testRestTemplate.exchange("/api/v1/rankings?page=2&size=10", HttpMethod.GET, null, responseType);

            // then
            assertAll(
                    () -> assertTrue(response2.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response2.getBody().data().rankings()).hasSize(10),
                    () -> assertThat(response2.getBody().data().page()).isEqualTo(2),
                    // 두 번째 페이지는 11위~20위
                    () -> assertThat(response2.getBody().data().rankings().get(0).rank()).isEqualTo(11),
                    () -> assertThat(response2.getBody().data().rankings().get(9).rank()).isEqualTo(20)
            );

            // when - page=3 (마지막 페이지)
            ResponseEntity<ApiResponse<RankingV1Dto.RankingPageResponse>> response3 =
                    testRestTemplate.exchange("/api/v1/rankings?page=3&size=10", HttpMethod.GET, null, responseType);

            // then
            assertAll(
                    () -> assertTrue(response3.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response3.getBody().data().rankings()).hasSize(5),  // 21~25위
                    () -> assertThat(response3.getBody().data().page()).isEqualTo(3),
                    () -> assertThat(response3.getBody().data().hasNext()).isFalse(),
                    () -> assertThat(response3.getBody().data().rankings().get(0).rank()).isEqualTo(21),
                    () -> assertThat(response3.getBody().data().rankings().get(4).rank()).isEqualTo(25)
            );
        }

        @DisplayName("랭킹 데이터가 없을 때 빈 배열을 반환한다")
        @Test
        void shouldReturnEmptyList_whenNoRankingData() {
            // given - 랭킹 데이터 없음

            // when
            ParameterizedTypeReference<ApiResponse<RankingV1Dto.RankingPageResponse>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<RankingV1Dto.RankingPageResponse>> response =
                    testRestTemplate.exchange("/api/v1/rankings", HttpMethod.GET, null, responseType);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data()).isNotNull(),
                    () -> assertThat(response.getBody().data().rankings()).isEmpty(),
                    () -> assertThat(response.getBody().data().totalCount()).isEqualTo(0),
                    () -> assertThat(response.getBody().data().hasNext()).isFalse()
            );
        }

        @DisplayName("date 파라미터로 특정 날짜의 랭킹을 조회할 수 있다")
        @Test
        void shouldReturnRankings_forSpecificDate() {
            // given - 어제 날짜의 랭킹 데이터 생성
            String yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            Product product = createProduct("어제상품", 10000);

            String yesterdayKey = CacheKeyGenerator.dailyRankingKey(yesterday);
            redisTemplate.opsForZSet().add(yesterdayKey, product.getId().toString(), 50.0);

            // when
            ParameterizedTypeReference<ApiResponse<RankingV1Dto.RankingPageResponse>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<RankingV1Dto.RankingPageResponse>> response =
                    testRestTemplate.exchange("/api/v1/rankings?date=" + yesterday, HttpMethod.GET, null, responseType);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody().data().date()).isEqualTo(yesterday),
                    () -> assertThat(response.getBody().data().rankings()).hasSize(1),
                    () -> assertThat(response.getBody().data().rankings().get(0).productName()).isEqualTo("어제상품")
            );
        }

        @DisplayName("존재하지 않는 상품은 랭킹에서 필터링된다")
        @Test
        void shouldFilterNonExistentProducts() {
            // given
            Product existingProduct = createProduct("존재하는상품", 10000);
            Long nonExistentProductId = 99999L;

            addToRanking(existingProduct.getId(), 100.0);
            addToRanking(nonExistentProductId, 80.0);  // DB에 없는 상품

            // when
            ParameterizedTypeReference<ApiResponse<RankingV1Dto.RankingPageResponse>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<RankingV1Dto.RankingPageResponse>> response =
                    testRestTemplate.exchange("/api/v1/rankings", HttpMethod.GET, null, responseType);

            // then - 존재하는 상품만 반환됨
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody().data().rankings()).hasSize(1),
                    () -> assertThat(response.getBody().data().rankings().get(0).productId()).isEqualTo(existingProduct.getId())
            );
        }
    }
}
