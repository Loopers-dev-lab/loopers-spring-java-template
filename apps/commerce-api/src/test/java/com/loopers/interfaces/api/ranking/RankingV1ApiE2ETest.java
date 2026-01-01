package com.loopers.interfaces.api.ranking;

import com.loopers.domain.brand.Brand;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.domain.product.LikeCount;
import com.loopers.domain.product.Price;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.Stock;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.interfaces.api.ranking.RankingV1Dto.RankingItem;
import com.loopers.interfaces.api.ranking.RankingV1Dto.RankingListResponse;
import com.loopers.support.IntegrationTest;
import com.loopers.utils.DatabaseCleanUp;
import com.loopers.utils.RedisCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RankingV1ApiE2ETest extends IntegrationTest {

    private static final String ENDPOINT_RANKING = "/api/v1/rankings";
    private static final String RANKING_KEY_PREFIX = "ranking:all:";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final TestRestTemplate testRestTemplate;
    private final BrandJpaRepository brandJpaRepository;
    private final ProductRepository productRepository;
    private final DatabaseCleanUp databaseCleanUp;
    private final RedisCleanUp redisCleanUp;

    @Autowired
    @Qualifier("redisTemplateMaster")
    private RedisTemplate<String, String> redisTemplateMaster;

    @Autowired
    public RankingV1ApiE2ETest(
            TestRestTemplate testRestTemplate,
            BrandJpaRepository brandJpaRepository,
            ProductRepository productRepository,
            DatabaseCleanUp databaseCleanUp,
            RedisCleanUp redisCleanUp
    ) {
        this.testRestTemplate = testRestTemplate;
        this.brandJpaRepository = brandJpaRepository;
        this.productRepository = productRepository;
        this.databaseCleanUp = databaseCleanUp;
        this.redisCleanUp = redisCleanUp;
    }

    @AfterEach
    void tearDown() {
        redisCleanUp.truncateAll();
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("GET /api/v1/rankings - 일별 랭킹 조회")
    @Nested
    class GetDailyRanking {

        @DisplayName("랭킹 데이터가 있으면 정상적으로 반환된다.")
        @Test
        void returnsRanking_whenDataExists() {
            // arrange
            Brand brand1 = createBrand("브랜드1");
            Brand brand2 = createBrand("브랜드2");
            brand1 = brandJpaRepository.save(brand1);
            brand2 = brandJpaRepository.save(brand2);

            Product product1 = createProduct("상품1", brand1.getId(), 10000, 10, 100);
            Product product2 = createProduct("상품2", brand2.getId(), 20000, 20, 200);
            productRepository.saveProduct(product1);
            productRepository.saveProduct(product2);

            LocalDate date = LocalDate.now();
            String key = getRankingKey(date);
            ZSetOperations<String, String> zSetOps = redisTemplateMaster.opsForZSet();
            zSetOps.add(key, String.valueOf(product1.getId()), 100.0);
            zSetOps.add(key, String.valueOf(product2.getId()), 50.0);

            String url = ENDPOINT_RANKING + "?date=" + date.format(DATE_FORMATTER) + "&type=DAILY&page=1&size=20";

            // act
            ResponseEntity<ApiResponse<RankingListResponse>> response = testRestTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().meta().result()).isEqualTo(ApiResponse.Metadata.Result.SUCCESS);

            RankingListResponse rankingResponse = response.getBody().data();
            assertThat(rankingResponse.rankings()).hasSize(2);

            RankingItem item1 = rankingResponse.rankings().get(0);
            assertAll(
                    () -> assertThat(item1.productId()).isEqualTo(product1.getId()),
                    () -> assertThat(item1.productName()).isEqualTo("상품1"),
                    () -> assertThat(item1.brandName()).isEqualTo("브랜드1"),
                    () -> assertThat(item1.price()).isEqualTo(10000),
                    () -> assertThat(item1.likeCount()).isEqualTo(10),
                    () -> assertThat(item1.rank()).isEqualTo(1)
            );

            RankingItem item2 = rankingResponse.rankings().get(1);
            assertAll(
                    () -> assertThat(item2.productId()).isEqualTo(product2.getId()),
                    () -> assertThat(item2.productName()).isEqualTo("상품2"),
                    () -> assertThat(item2.brandName()).isEqualTo("브랜드2"),
                    () -> assertThat(item2.price()).isEqualTo(20000),
                    () -> assertThat(item2.likeCount()).isEqualTo(20),
                    () -> assertThat(item2.rank()).isEqualTo(2)
            );
        }

        @DisplayName("랭킹 데이터가 없으면 빈 리스트를 반환한다.")
        @Test
        void returnsEmptyList_whenNoDataExists() {
            // arrange
            LocalDate date = LocalDate.now();
            String url = ENDPOINT_RANKING + "?date=" + date.format(DATE_FORMATTER) + "&type=DAILY&page=1&size=20";

            // act
            ResponseEntity<ApiResponse<RankingListResponse>> response = testRestTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().meta().result()).isEqualTo(ApiResponse.Metadata.Result.SUCCESS);

            RankingListResponse rankingResponse = response.getBody().data();
            assertThat(rankingResponse.rankings()).isEmpty();
        }

        @DisplayName("페이징이 정상적으로 동작한다.")
        @Test
        void returnsPaginatedResults() {
            // arrange
            Brand brand = createBrand("브랜드1");
            brand = brandJpaRepository.save(brand);

            Product product1 = createProduct("상품1", brand.getId(), 10000, 10, 100);
            Product product2 = createProduct("상품2", brand.getId(), 20000, 20, 200);
            Product product3 = createProduct("상품3", brand.getId(), 30000, 30, 300);
            productRepository.saveProduct(product1);
            productRepository.saveProduct(product2);
            productRepository.saveProduct(product3);

            LocalDate date = LocalDate.now();
            String key = getRankingKey(date);
            ZSetOperations<String, String> zSetOps = redisTemplateMaster.opsForZSet();
            zSetOps.add(key, String.valueOf(product1.getId()), 100.0);
            zSetOps.add(key, String.valueOf(product2.getId()), 50.0);
            zSetOps.add(key, String.valueOf(product3.getId()), 30.0);

            // act - 첫 페이지
            String url1 = ENDPOINT_RANKING + "?date=" + date.format(DATE_FORMATTER) + "&type=DAILY&page=1&size=2";
            ResponseEntity<ApiResponse<RankingListResponse>> response1 = testRestTemplate.exchange(
                    url1,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.OK);
            RankingListResponse page1 = response1.getBody().data();
            assertThat(page1.rankings()).hasSize(2);
            assertThat(page1.rankings().get(0).productId()).isEqualTo(product1.getId());
            assertThat(page1.rankings().get(1).productId()).isEqualTo(product2.getId());

            // act - 두 번째 페이지
            String url2 = ENDPOINT_RANKING + "?date=" + date.format(DATE_FORMATTER) + "&type=DAILY&page=2&size=2";
            ResponseEntity<ApiResponse<RankingListResponse>> response2 = testRestTemplate.exchange(
                    url2,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.OK);
            RankingListResponse page2 = response2.getBody().data();
            assertThat(page2.rankings()).hasSize(1);
            assertThat(page2.rankings().get(0).productId()).isEqualTo(product3.getId());
        }
    }

    private Brand createBrand(String name) {
        return Brand.createBrand(name, name + " 설명");
    }

    private Product createProduct(String name, Long brandId, Integer price, Integer likeCount, Integer stock) {
        return Product.createProduct(
                name,
                brandId,
                Price.createPrice(price),
                LikeCount.createLikeCount(likeCount),
                Stock.createStock(stock)
        );
    }

    private String getRankingKey(LocalDate date) {
        return RANKING_KEY_PREFIX + date.format(DATE_FORMATTER);
    }
}

