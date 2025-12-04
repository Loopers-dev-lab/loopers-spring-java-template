package com.loopers.domain.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.loopers.utils.DatabaseCleanUp;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class ProductServiceIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("상품 목록을 조회할 때,")
    @Nested
    class FindProducts {

        @DisplayName("최신순 정렬 시 createdAt 내림차순으로 반환된다.")
        @Test
        void returnsProductsSortedByLatest() throws Exception {
            // arrange
            Long brandId = 1L;
            int page = 0;
            int size = 10;

            Product product1 = Product.createProduct(
                    "상품1", brandId,
                    Price.createPrice(10000),
                    LikeCount.createLikeCount(10),
                    Stock.createStock(100)
            );
            productRepository.saveProduct(product1);
            Thread.sleep(10);

            Product product2 = Product.createProduct(
                    "상품2", brandId,
                    Price.createPrice(20000),
                    LikeCount.createLikeCount(20),
                    Stock.createStock(200)
            );
            productRepository.saveProduct(product2);
            Thread.sleep(10);

            Product product3 = Product.createProduct(
                    "상품3", brandId,
                    Price.createPrice(30000),
                    LikeCount.createLikeCount(30),
                    Stock.createStock(300)
            );
            productRepository.saveProduct(product3);

            // act
            List<Product> result = productService.findProductsByLatest(brandId, page, size);

            // assert
            assertThat(result).hasSize(3);
            assertAll(
                    () -> assertThat(result.get(0).getName()).isEqualTo("상품3"),
                    () -> assertThat(result.get(1).getName()).isEqualTo("상품2"),
                    () -> assertThat(result.get(2).getName()).isEqualTo("상품1")
            );
        }

        @DisplayName("가격 오름차순 정렬 시 price 오름차순으로 반환된다.")
        @Test
        void returnsProductsSortedByPriceAsc() throws Exception {
            // arrange
            Long brandId = 1L;
            int page = 0;
            int size = 10;

            Product product1 = Product.createProduct(
                    "상품1", brandId,
                    Price.createPrice(30000),
                    LikeCount.createLikeCount(10),
                    Stock.createStock(100)
            );
            productRepository.saveProduct(product1);

            Product product2 = Product.createProduct(
                    "상품2", brandId,
                    Price.createPrice(10000),
                    LikeCount.createLikeCount(20),
                    Stock.createStock(200)
            );
            productRepository.saveProduct(product2);

            Product product3 = Product.createProduct(
                    "상품3", brandId,
                    Price.createPrice(20000),
                    LikeCount.createLikeCount(30),
                    Stock.createStock(300)
            );
            productRepository.saveProduct(product3);

            // act
            List<Product> result = productService.findProductsByPriceAsc(brandId, page, size);

            // assert
            assertThat(result).hasSize(3);
            assertAll(
                    () -> assertThat(result.get(0).getName()).isEqualTo("상품2"),
                    () -> assertThat(result.get(0).getPrice().getPrice()).isEqualTo(10000),
                    () -> assertThat(result.get(1).getName()).isEqualTo("상품3"),
                    () -> assertThat(result.get(1).getPrice().getPrice()).isEqualTo(20000),
                    () -> assertThat(result.get(2).getName()).isEqualTo("상품1"),
                    () -> assertThat(result.get(2).getPrice().getPrice()).isEqualTo(30000)
            );
        }

        @DisplayName("좋아요 내림차순 정렬 시 likeCount 내림차순으로 반환된다.")
        @Test
        void returnsProductsSortedByLikesDesc() throws Exception {
            // arrange
            Long brandId = 1L;
            int page = 0;
            int size = 10;

            Product product1 = Product.createProduct(
                    "상품1", brandId,
                    Price.createPrice(10000),
                    LikeCount.createLikeCount(30),
                    Stock.createStock(100)
            );
            productRepository.saveProduct(product1);

            Product product2 = Product.createProduct(
                    "상품2", brandId,
                    Price.createPrice(20000),
                    LikeCount.createLikeCount(10),
                    Stock.createStock(200)
            );
            productRepository.saveProduct(product2);

            Product product3 = Product.createProduct(
                    "상품3", brandId,
                    Price.createPrice(30000),
                    LikeCount.createLikeCount(50),
                    Stock.createStock(300)
            );
            productRepository.saveProduct(product3);

            // act
            List<Product> result = productService.findProductsByLikesDesc(brandId, page, size);

            // assert
            assertThat(result).hasSize(3);
            assertAll(
                    () -> assertThat(result.get(0).getName()).isEqualTo("상품3"),
                    () -> assertThat(result.get(0).getLikeCount().getCount()).isEqualTo(50),
                    () -> assertThat(result.get(1).getName()).isEqualTo("상품1"),
                    () -> assertThat(result.get(1).getLikeCount().getCount()).isEqualTo(30),
                    () -> assertThat(result.get(2).getName()).isEqualTo("상품2"),
                    () -> assertThat(result.get(2).getLikeCount().getCount()).isEqualTo(10)
            );
        }

        @DisplayName("isDeleted가 true인 상품은 조회되지 않는다.")
        @Test
        void excludesDeletedProducts() throws Exception {
            // arrange
            Long brandId = 1L;
            int page = 0;
            int size = 10;

            Product activeProduct1 = Product.createProduct(
                    "상품1", brandId,
                    Price.createPrice(10000),
                    LikeCount.createLikeCount(10),
                    Stock.createStock(100)
            );
            productRepository.saveProduct(activeProduct1);

            Product activeProduct2 = Product.createProduct(
                    "상품2", brandId,
                    Price.createPrice(20000),
                    LikeCount.createLikeCount(20),
                    Stock.createStock(200)
            );
            productRepository.saveProduct(activeProduct2);

            Product deletedProduct = Product.createProduct(
                    "삭제된상품", brandId,
                    Price.createPrice(30000),
                    LikeCount.createLikeCount(30),
                    Stock.createStock(300)
            );
            productRepository.saveProduct(deletedProduct);
            setDeleted(deletedProduct, true);
            productRepository.saveProduct(deletedProduct);

            // act
            List<Product> result = productService.findProductsByLatest(brandId, page, size);

            // assert
            assertThat(result).hasSize(2);
            assertThat(result).extracting(Product::getName)
                    .containsExactlyInAnyOrder("상품1", "상품2");
            assertThat(result).extracting(Product::getName)
                    .doesNotContain("삭제된상품");
        }

        @DisplayName("페이지네이션이 올바르게 동작한다.")
        @Test
        void paginationWorksCorrectly() throws Exception {
            // arrange
            Long brandId = 1L;

            // 5개의 상품 생성
            for (int i = 1; i <= 5; i++) {
                Product product = Product.createProduct(
                        "상품" + i, brandId,
                        Price.createPrice(10000 * i),
                        LikeCount.createLikeCount(10 * i),
                        Stock.createStock(100 * i)
                );
                productRepository.saveProduct(product);
                Thread.sleep(10);
            }

            // act
            List<Product> page1 = productService.findProductsByLatest(brandId, 0, 2);
            List<Product> page2 = productService.findProductsByLatest(brandId, 1, 2);
            List<Product> page3 = productService.findProductsByLatest(brandId, 2, 2);

            // assert
            assertThat(page1).hasSize(2);
            assertThat(page2).hasSize(2);
            assertThat(page3).hasSize(1);

            assertThat(page1.get(0).getName()).isEqualTo("상품5");
            assertThat(page1.get(1).getName()).isEqualTo("상품4");
            assertThat(page2.get(0).getName()).isEqualTo("상품3");
            assertThat(page2.get(1).getName()).isEqualTo("상품2");
            assertThat(page3.get(0).getName()).isEqualTo("상품1");
        }

        @DisplayName("brandId로 필터링이 올바르게 동작한다.")
        @Test
        void filtersByBrandId() throws Exception {
            // arrange
            Long brandId1 = 1L;
            Long brandId2 = 2L;
            int page = 0;
            int size = 10;

            Product product1 = Product.createProduct(
                    "브랜드1상품1", brandId1,
                    Price.createPrice(10000),
                    LikeCount.createLikeCount(10),
                    Stock.createStock(100)
            );
            productRepository.saveProduct(product1);

            Product product2 = Product.createProduct(
                    "브랜드1상품2", brandId1,
                    Price.createPrice(20000),
                    LikeCount.createLikeCount(20),
                    Stock.createStock(200)
            );
            productRepository.saveProduct(product2);

            Product product3 = Product.createProduct(
                    "브랜드2상품1", brandId2,
                    Price.createPrice(30000),
                    LikeCount.createLikeCount(30),
                    Stock.createStock(300)
            );
            productRepository.saveProduct(product3);

            // act
            List<Product> result = productService.findProductsByLatest(brandId1, page, size);

            // assert
            assertThat(result).hasSize(2);
            assertThat(result).extracting(Product::getBrandId)
                    .containsOnly(brandId1);
            assertThat(result).extracting(Product::getName)
                    .containsExactlyInAnyOrder("브랜드1상품1", "브랜드1상품2");
        }
    }

    private void setDeleted(Product product, boolean isDeleted) throws Exception {
        Field field = Product.class.getDeclaredField("isDeleted");
        field.setAccessible(true);
        field.setBoolean(product, isDeleted);
    }
}
