package com.loopers.application.ranking;

import com.loopers.application.ranking.RankingCommand.GetRankingCommand;
import com.loopers.application.ranking.RankingCommand.RankingType;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.LikeCount;
import com.loopers.domain.product.Price;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.Stock;
import com.loopers.infrastructure.brand.BrandJpaRepository;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RankingFacadeIntegrationTest extends IntegrationTest {

    @Autowired
    private RankingFacade rankingFacade;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandJpaRepository brandJpaRepository;

    @Autowired
    @Qualifier("redisTemplateMaster")
    private RedisTemplate<String, String> redisTemplateMaster;

    @Autowired
    private RedisCleanUp redisCleanUp;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private static final String RANKING_KEY_PREFIX = "ranking:all:";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @AfterEach
    void tearDown() {
        redisCleanUp.truncateAll();
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("일별 랭킹 조회")
    @Nested
    class GetDailyRanking {

        @DisplayName("랭킹 데이터가 있고 상품 정보가 있으면 정상적으로 조회된다.")
        @Test
        void returnsRankingInfo_whenDataExists() {
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

            GetRankingCommand command = new GetRankingCommand(
                    date, RankingType.DAILY, 1, 20
            );

            // act
            RankingInfo result = rankingFacade.getRanking(command);

            // assert
            assertThat(result.items()).hasSize(2);
            RankingInfo.RankingItemInfo item1 = result.items().get(0);
            assertThat(item1.productId()).isEqualTo(product1.getId());
            assertThat(item1.productName()).isEqualTo("상품1");
            assertThat(item1.brandName()).isEqualTo("브랜드1");
            assertThat(item1.price()).isEqualTo(10000);
            assertThat(item1.likeCount()).isEqualTo(10);
            assertThat(item1.rank()).isEqualTo(1L);
            assertThat(item1.score()).isEqualTo(100.0);

            RankingInfo.RankingItemInfo item2 = result.items().get(1);
            assertThat(item2.productId()).isEqualTo(product2.getId());
            assertThat(item2.productName()).isEqualTo("상품2");
            assertThat(item2.brandName()).isEqualTo("브랜드2");
            assertThat(item2.price()).isEqualTo(20000);
            assertThat(item2.likeCount()).isEqualTo(20);
            assertThat(item2.rank()).isEqualTo(2L);
            assertThat(item2.score()).isEqualTo(50.0);
        }

        @DisplayName("랭킹에 있지만 DB에 없는 상품은 제외된다.")
        @Test
        void excludesProducts_notInDatabase() {
            // arrange
            Brand brand = createBrand("브랜드1");
            brand = brandJpaRepository.save(brand);

            Product product = createProduct("상품1", brand.getId(), 10000, 10, 100);
            productRepository.saveProduct(product);

            LocalDate date = LocalDate.now();
            String key = getRankingKey(date);
            ZSetOperations<String, String> zSetOps = redisTemplateMaster.opsForZSet();
            zSetOps.add(key, String.valueOf(product.getId()), 100.0);
            zSetOps.add(key, "999", 50.0); // DB에 없는 상품

            GetRankingCommand command = new GetRankingCommand(
                    date, RankingType.DAILY, 1, 20
            );

            // act
            RankingInfo result = rankingFacade.getRanking(command);

            // assert
            assertThat(result.items()).hasSize(1);
            assertThat(result.items().get(0).productId()).isEqualTo(product.getId());
        }

        @DisplayName("랭킹 데이터가 없으면 빈 리스트를 반환한다.")
        @Test
        void returnsEmptyList_whenNoRankingData() {
            // arrange
            GetRankingCommand command = new GetRankingCommand(
                    LocalDate.now(), RankingType.DAILY, 1, 20
            );

            // act
            RankingInfo result = rankingFacade.getRanking(command);

            // assert
            assertThat(result.items()).isEmpty();
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
            RankingInfo page1 = rankingFacade.getRanking(
                    new GetRankingCommand(date, RankingType.DAILY, 1, 2)
            );

            // assert
            assertThat(page1.items()).hasSize(2);
            assertThat(page1.items().get(0).productId()).isEqualTo(product1.getId());
            assertThat(page1.items().get(1).productId()).isEqualTo(product2.getId());

            // act - 두 번째 페이지
            RankingInfo page2 = rankingFacade.getRanking(
                    new GetRankingCommand(date, RankingType.DAILY, 2, 2)
            );

            // assert
            assertThat(page2.items()).hasSize(1);
            assertThat(page2.items().get(0).productId()).isEqualTo(product3.getId());
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

