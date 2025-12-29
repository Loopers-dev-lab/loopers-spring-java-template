package com.loopers.interfaces.api.ranking;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;
import com.loopers.fixture.TestFixture;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.utils.DatabaseCleanUp;
import com.loopers.utils.RedisCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RankingV1ApiE2ETest {

    private static final String ENDPOINT_RANKINGS = "/api/v1/rankings";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private RedisCleanUp redisCleanUp;

    @Autowired
    private TestFixture testFixture;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private Brand savedBrand;
    private Product savedProduct1;
    private Product savedProduct2;
    private Product savedProduct3;
    private String todayKey;

    @BeforeEach
    void setUp() {
        // 데이터 초기화
        databaseCleanUp.truncateAllTables();
        redisCleanUp.truncateAll();

        // 테스트 데이터 생성
        savedBrand = testFixture.createBrand("Nike");
        savedProduct1 = testFixture.createProduct("Air Max", 150000L, 100, savedBrand);
        savedProduct2 = testFixture.createProduct("Air Force", 120000L, 100, savedBrand);
        savedProduct3 = testFixture.createProduct("Jordan", 200000L, 100, savedBrand);

        // Redis에 랭킹 데이터 생성
        LocalDate today = LocalDate.now();
        todayKey = "ranking:all:" + today.format(DATE_FORMATTER);

        redisTemplate.opsForZSet().add(todayKey, savedProduct1.getId().toString(), 10.5);
        redisTemplate.opsForZSet().add(todayKey, savedProduct2.getId().toString(), 8.3);
        redisTemplate.opsForZSet().add(todayKey, savedProduct3.getId().toString(), 15.2);
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
        redisCleanUp.truncateAll();
    }

    @Nested
    @DisplayName("GET /api/v1/rankings")
    class GetRankings {

        @Test
        @DisplayName("랭킹 페이지 조회에 성공하면 상품 정보가 포함된 랭킹 목록을 반환한다")
        void shouldReturnRankingPageWithProductInfo() {
            // given
            String today = LocalDate.now().format(DATE_FORMATTER);
            String url = String.format("%s?date=%s&page=0&size=10", ENDPOINT_RANKINGS, today);

            // when
            ParameterizedTypeReference<ApiResponse<RankingV1Dto.RankingPageResponse>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<RankingV1Dto.RankingPageResponse>> response =
                    testRestTemplate.exchange(url, HttpMethod.GET, null, responseType);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data().rankings()).hasSize(3),
                    () -> assertThat(response.getBody().data().rankings().get(0).productName()).isEqualTo("Jordan"),
                    () -> assertThat(response.getBody().data().rankings().get(0).rank()).isEqualTo(1L),
                    () -> assertThat(response.getBody().data().rankings().get(0).score()).isEqualTo(15.2)
            );
        }

        @Test
        @DisplayName("페이지네이션이 정상 동작한다")
        void shouldReturnPaginatedResults() {
            // given
            String today = LocalDate.now().format(DATE_FORMATTER);
            String url = String.format("%s?date=%s&page=0&size=2", ENDPOINT_RANKINGS, today);

            // when
            ParameterizedTypeReference<ApiResponse<RankingV1Dto.RankingPageResponse>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<RankingV1Dto.RankingPageResponse>> response =
                    testRestTemplate.exchange(url, HttpMethod.GET, null, responseType);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody().data().rankings()).hasSize(2),
                    () -> assertThat(response.getBody().data().totalCount()).isEqualTo(3L),
                    () -> assertThat(response.getBody().data().totalPages()).isEqualTo(2)
            );
        }

        @Test
        @DisplayName("date 파라미터가 없으면 오늘 날짜로 조회한다")
        void shouldUseCurrentDateWhenDateIsNotProvided() {
            // given
            String url = String.format("%s?page=0&size=10", ENDPOINT_RANKINGS);

            // when
            ParameterizedTypeReference<ApiResponse<RankingV1Dto.RankingPageResponse>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<RankingV1Dto.RankingPageResponse>> response =
                    testRestTemplate.exchange(url, HttpMethod.GET, null, responseType);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody().data().date()).isEqualTo(LocalDate.now().format(DATE_FORMATTER))
            );
        }

        @Test
        @DisplayName("랭킹 데이터가 없는 날짜 조회 시 빈 목록을 반환한다")
        void shouldReturnEmptyListWhenNoRankingData() {
            // given
            String futureDate = LocalDate.now().plusDays(10).format(DATE_FORMATTER);
            String url = String.format("%s?date=%s&page=0&size=10", ENDPOINT_RANKINGS, futureDate);

            // when
            ParameterizedTypeReference<ApiResponse<RankingV1Dto.RankingPageResponse>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<RankingV1Dto.RankingPageResponse>> response =
                    testRestTemplate.exchange(url, HttpMethod.GET, null, responseType);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody().data().rankings()).isEmpty(),
                    () -> assertThat(response.getBody().data().totalCount()).isEqualTo(0L)
            );
        }

        @Test
        @DisplayName("유효하지 않은 size 값은 기본값으로 처리된다")
        void shouldUseDefaultSizeWhenInvalid() {
            // given
            String today = LocalDate.now().format(DATE_FORMATTER);
            String url = String.format("%s?date=%s&page=0&size=0", ENDPOINT_RANKINGS, today);

            // when
            ParameterizedTypeReference<ApiResponse<RankingV1Dto.RankingPageResponse>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<RankingV1Dto.RankingPageResponse>> response =
                    testRestTemplate.exchange(url, HttpMethod.GET, null, responseType);

            // then
            assertTrue(response.getStatusCode().is2xxSuccessful());
            assertThat(response.getBody().data().rankings()).isNotEmpty();        }
    }

    @Nested
    @DisplayName("GET /api/v1/rankings/top")
    class GetTopN {

        @Test
        @DisplayName("Top-N 랭킹을 조회한다")
        void shouldReturnTopN() {
            // given
            String today = LocalDate.now().format(DATE_FORMATTER);
            String url = String.format("%s/top?date=%s&n=2", ENDPOINT_RANKINGS, today);

            // when
            ParameterizedTypeReference<ApiResponse<RankingV1Dto.TopNResponse>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<RankingV1Dto.TopNResponse>> response =
                    testRestTemplate.exchange(url, HttpMethod.GET, null, responseType);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody().data().rankings()).hasSize(2),
                    () -> assertThat(response.getBody().data().rankings().get(0).productName()).isEqualTo("Jordan")
            );
        }
    }
}
