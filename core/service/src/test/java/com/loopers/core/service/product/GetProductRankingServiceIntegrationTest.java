package com.loopers.core.service.product;

import com.loopers.core.domain.brand.Brand;
import com.loopers.core.domain.brand.BrandFixture;
import com.loopers.core.domain.brand.repository.BrandRepository;
import com.loopers.core.domain.brand.vo.BrandId;
import com.loopers.core.domain.product.Product;
import com.loopers.core.domain.product.ProductFixture;
import com.loopers.core.domain.product.ProductRankingList;
import com.loopers.core.domain.product.repository.ProductRankingCacheRepository;
import com.loopers.core.domain.product.repository.ProductRepository;
import com.loopers.core.domain.product.vo.ProductId;
import com.loopers.core.service.IntegrationTest;
import com.loopers.core.service.product.query.GetProductRankingQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

@DisplayName("상품 랭킹 조회 통합 테스트")
class GetProductRankingServiceIntegrationTest extends IntegrationTest {

    @Autowired
    private GetProductRankingService getProductRankingService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductRankingCacheRepository productRankingCacheRepository;

    @Autowired
    private BrandRepository brandRepository;

    private LocalDate testDate;
    private List<Product> testProducts;
    private Brand testBrand;

    @BeforeEach
    void setUp() {
        testDate = LocalDate.now();

        testBrand = BrandFixture.create();
        Brand savedBrand = brandRepository.save(testBrand);

        testProducts = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Product product = ProductFixture.createWith(new BrandId(savedBrand.getId().value()));
            testProducts.add(product);
        }
        List<Product> savedProducts = productRepository.saveAll(testProducts);
        testProducts = savedProducts;
    }

    @Nested
    @DisplayName("랭킹 조회")
    class GetRanking {

        @Test
        @DisplayName("페이지네이션이 적용된다")
        void getRankingWithPagination() {
            // Given
            for (int i = 0; i < 5; i++) {
                ProductId productId = testProducts.get(i).getId();
                productRankingCacheRepository.increaseDaily(productId, testDate, (double) (500 - i * 100));
            }

            GetProductRankingQuery query = new GetProductRankingQuery(testDate, 0, 2);

            // When
            ProductRankingList result = getProductRankingService.getRanking(query);

            // Then
            assertSoftly(softAssertions -> {
                softAssertions.assertThat(result.products()).hasSize(2);
                softAssertions.assertThat(result.totalElements()).isEqualTo(5);
                softAssertions.assertThat(result.totalPages()).isEqualTo(3);
                softAssertions.assertThat(result.hasNext()).isTrue();
                softAssertions.assertThat(result.hasPrevious()).isFalse();
            });
        }

        @Test
        @DisplayName("랭킹에 없는 상품들은 조회되지 않는다")
        void getRankingWithPartialProducts() {
            // Given
            ProductId product1Id = testProducts.get(0).getId();
            ProductId product2Id = testProducts.get(1).getId();

            productRankingCacheRepository.increaseDaily(product1Id, testDate, 100.0);
            productRankingCacheRepository.increaseDaily(product2Id, testDate, 200.0);

            GetProductRankingQuery query = new GetProductRankingQuery(testDate, 0, 10);

            // When
            ProductRankingList result = getProductRankingService.getRanking(query);

            // Then
            assertThat(result.products()).hasSize(2);
        }

        @Test
        @DisplayName("랭킹이 비어있으면 빈 목록이 반환된다")
        void getRankingWhenEmpty() {
            // Given
            GetProductRankingQuery query = new GetProductRankingQuery(testDate, 0, 10);

            // When
            ProductRankingList result = getProductRankingService.getRanking(query);

            // Then
            assertSoftly(softAssertions -> {
                softAssertions.assertThat(result.products()).isEmpty();
                softAssertions.assertThat(result.totalElements()).isEqualTo(0);
                softAssertions.assertThat(result.totalPages()).isEqualTo(0);
                softAssertions.assertThat(result.hasNext()).isFalse();
                softAssertions.assertThat(result.hasPrevious()).isFalse();
            });
        }

        @Test
        @DisplayName("상품 정보가 정확하게 조회된다")
        void getRankingWithCorrectProductInfo() {
            // Given
            ProductId product1Id = testProducts.get(0).getId();
            productRankingCacheRepository.increaseDaily(product1Id, testDate, 100.0);

            GetProductRankingQuery query = new GetProductRankingQuery(testDate, 0, 10);

            // When
            ProductRankingList result = getProductRankingService.getRanking(query);

            // Then
            assertSoftly(softAssertions -> {
                softAssertions.assertThat(result.products()).hasSize(1);
                softAssertions.assertThat(result.products().get(0).id()).isEqualTo(product1Id);
                softAssertions.assertThat(result.products().get(0).name()).isNotNull();
                softAssertions.assertThat(result.products().get(0).price()).isNotNull();
                softAssertions.assertThat(result.products().get(0).likeCount()).isNotNull();
                softAssertions.assertThat(result.products().get(0).brandName()).isNotNull();
            });
        }
    }
}
