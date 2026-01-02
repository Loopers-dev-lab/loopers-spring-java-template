package com.loopers.interfaces.api.ranking;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RankingApiE2ETest {

    private static final String ENDPOINT = "/api/v1/rankings";

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private BrandJpaRepository brandJpaRepository;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
        // Redis ZSET 정리
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        redisTemplate.delete("ranking:all:" + today);
    }

    @Test
    @DisplayName("랭킹 조회 시 점수가 높은 순으로 상품 목록을 반환한다")
    void rankingTest1() {
        // arrange
        Brand brand = brandJpaRepository.save(Brand.create("브랜드A"));

        Product productA = productJpaRepository.save(
                Product.create("상품A", "설명", 10000, 100L, brand.getId()));
        Product productB = productJpaRepository.save(
                Product.create("상품B", "설명", 20000, 100L, brand.getId()));
        Product productC = productJpaRepository.save(
                Product.create("상품C", "설명", 30000, 100L, brand.getId()));

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String key = "ranking:all:" + today;

        // Redis ZSET에 점수 저장
        redisTemplate.opsForZSet().add(key, productA.getId().toString(), 3.0);
        redisTemplate.opsForZSet().add(key, productB.getId().toString(), 5.0);
        redisTemplate.opsForZSet().add(key, productC.getId().toString(), 1.0);

        // act
        ParameterizedTypeReference<ApiResponse<RankingDto.RankingListResponse>> type =
                new ParameterizedTypeReference<>() {};

        ResponseEntity<ApiResponse<RankingDto.RankingListResponse>> response =
                testRestTemplate.exchange(ENDPOINT, HttpMethod.GET, null, type);

        // assert
        // 예상 순서: 상품B(5점) > 상품A(3점) > 상품C(1점)
        assertAll(
                () -> assertThat(response.getStatusCode().is2xxSuccessful()).isTrue(),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().data().content()).hasSize(3),
                () -> assertThat(response.getBody().data().content().get(0).productId()).isEqualTo(productB.getId()),
                () -> assertThat(response.getBody().data().content().get(0).rank()).isEqualTo(1),
                () -> assertThat(response.getBody().data().content().get(1).productId()).isEqualTo(productA.getId()),
                () -> assertThat(response.getBody().data().content().get(2).productId()).isEqualTo(productC.getId())
        );
    }
}
