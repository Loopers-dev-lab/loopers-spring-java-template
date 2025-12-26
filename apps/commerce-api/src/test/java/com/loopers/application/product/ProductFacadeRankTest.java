package com.loopers.application.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.ranking.RankingRedisService;
import com.loopers.interfaces.api.product.ProductDto;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ProductFacadeRankTest {

    @Autowired
    private ProductFacade productFacade;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandJpaRepository brandJpaRepository;

    @Autowired
    private RankingRedisService rankingRedisService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private static final LocalDate TODAY = LocalDate.now();
    private static final String TODAY_KEY = "ranking:all:" + TODAY.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

    private Brand brand;
    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() {
        redisTemplate.delete(TODAY_KEY);

        brand = brandJpaRepository.save(Brand.create("테스트브랜드"));
        product1 = productRepository.save(Product.create("상품1", "설명1", 10000L, 100L, brand.getId()));
        product2 = productRepository.save(Product.create("상품2", "설명2", 20000L, 100L, brand.getId()));
    }

    @AfterEach
    void tearDown() {
        redisTemplate.delete(TODAY_KEY);
        databaseCleanUp.truncateAllTables();
    }

    @Test
    @DisplayName("랭킹에 있는 상품 조회 시 순위가 반환된다")
    void getProductRankTest1() {
        // arrange - 상품2가 1위, 상품1이 2위
        rankingRedisService.incrementScoreForOrder(TODAY, product2.getId(), 2L);  // 1.2점
        rankingRedisService.incrementScoreForView(TODAY, product1.getId());  // 0.1점

        // act
        ProductDto.ProductDetailResponse response = productFacade.getProduct(product1.getId());

        // assert
        assertThat(response.rank()).isEqualTo(2L);
    }

    @Test
    @DisplayName("랭킹 1위 상품 조회 시 rank=1이 반환된다")
    void getProductRankTest2() {
        // arrange - 상품1이 1위
        rankingRedisService.incrementScoreForOrder(TODAY, product1.getId(), 3L);  // 1.8점

        // act
        ProductDto.ProductDetailResponse response = productFacade.getProduct(product1.getId());

        // assert
        assertThat(response.rank()).isEqualTo(1L);
    }

    @Test
    @DisplayName("랭킹에 없는 상품 조회 시 rank가 null이다")
    void getProductRankTest3() {
        // arrange - 랭킹에 아무것도 없음

        // act
        ProductDto.ProductDetailResponse response = productFacade.getProduct(product1.getId());

        // assert
        assertThat(response.rank()).isNull();
    }
}
