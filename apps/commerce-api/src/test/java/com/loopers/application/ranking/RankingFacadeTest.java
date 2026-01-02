package com.loopers.application.ranking;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.ranking.RankingRedisService;
import com.loopers.interfaces.api.ranking.RankingDto;
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
class RankingFacadeTest {

    @Autowired
    private RankingFacade rankingFacade;

    @Autowired
    private RankingRedisService rankingRedisService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandJpaRepository brandJpaRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private static final LocalDate TODAY = LocalDate.now();
    private static final String TODAY_KEY = "ranking:all:" + TODAY.format(DateTimeFormatter.ofPattern("yyyyMMdd"));

    private Brand brand;
    private Product product1;
    private Product product2;
    private Product product3;

    @BeforeEach
    void setUp() {
        redisTemplate.delete(TODAY_KEY);

        brand = brandJpaRepository.save(Brand.create("테스트브랜드"));
        product1 = productRepository.save(Product.create("상품1", "설명1", 10000L, 100L, brand.getId()));
        product2 = productRepository.save(Product.create("상품2", "설명2", 20000L, 100L, brand.getId()));
        product3 = productRepository.save(Product.create("상품3", "설명3", 30000L, 100L, brand.getId()));
    }

    @AfterEach
    void tearDown() {
        redisTemplate.delete(TODAY_KEY);
        databaseCleanUp.truncateAllTables();
    }

    @Test
    @DisplayName("랭킹 조회 시 점수 높은 순으로 상품 정보와 함께 반환한다")
    void getRankingsTest1() {
        // arrange
        rankingRedisService.incrementScoreForView(TODAY, product1.getId());  // 0.1점
        rankingRedisService.incrementScoreForLike(TODAY, product2.getId()); // 0.2점
        rankingRedisService.incrementScoreForOrder(TODAY, product3.getId(), 1L); // 0.6점

        // act
        RankingDto.RankingListResponse response = rankingFacade.getRankings("daily", null, 0, 20);

        // assert
        assertThat(response.content()).hasSize(3);
        assertThat(response.content().get(0).productId()).isEqualTo(product3.getId()); // 1위: 0.6점
        assertThat(response.content().get(0).rank()).isEqualTo(1);
        assertThat(response.content().get(0).productName()).isEqualTo("상품3");
        assertThat(response.content().get(1).productId()).isEqualTo(product2.getId()); // 2위: 0.2점
        assertThat(response.content().get(2).productId()).isEqualTo(product1.getId()); // 3위: 0.1점
    }

    @Test
    @DisplayName("랭킹 조회 시 전체 개수를 반환한다")
    void getRankingsTest2() {
        // arrange
        rankingRedisService.incrementScoreForView(TODAY, product1.getId());
        rankingRedisService.incrementScoreForView(TODAY, product2.getId());
        rankingRedisService.incrementScoreForView(TODAY, product3.getId());

        // act
        RankingDto.RankingListResponse response = rankingFacade.getRankings("daily", null, 0, 2);

        // assert
        assertThat(response.content()).hasSize(2);  // 페이지 사이즈만큼
        assertThat(response.totalElements()).isEqualTo(3);  // 전체 개수
    }

    @Test
    @DisplayName("랭킹이 비어있으면 빈 목록을 반환한다")
    void getRankingsTest3() {
        // act
        RankingDto.RankingListResponse response = rankingFacade.getRankings("daily", null, 0, 20);

        // assert
        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isEqualTo(0);
    }

    @Test
    @DisplayName("페이지네이션이 정상 동작한다")
    void getRankingsTest4() {
        // arrange
        rankingRedisService.incrementScoreForOrder(TODAY, product1.getId(), 3L); // 1.8점 - 1위
        rankingRedisService.incrementScoreForOrder(TODAY, product2.getId(), 2L); // 1.2점 - 2위
        rankingRedisService.incrementScoreForOrder(TODAY, product3.getId(), 1L); // 0.6점 - 3위

        // act - 2페이지 (size=2, page=1)
        RankingDto.RankingListResponse response = rankingFacade.getRankings("daily", null, 1, 2);

        // assert
        assertThat(response.content()).hasSize(1);  // 3번째 상품만
        assertThat(response.content().get(0).productId()).isEqualTo(product3.getId());
        assertThat(response.content().get(0).rank()).isEqualTo(3);  // 3위
    }
}
