package com.loopers.domain.product;

import com.loopers.application.product.ProductDetailInfo;
import com.loopers.application.product.ProductFacade;
import com.loopers.domain.BaseEntity;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandService;
import com.loopers.domain.common.vo.Money;
import com.loopers.domain.stock.Stock;
import com.loopers.domain.stock.StockRepository;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
class ProductServiceIntegrationTest {
    @Autowired
    private ProductService productService;

    @Autowired
    private BrandService brandService;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private BrandJpaRepository brandJpaRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    ProductFacade productFacade;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @BeforeEach
    void setUp() {
        int brandCount = 4;
        long brandId = 1;
        for(int i=1; i<=40; i++) {
            brandId = (i - 1) % brandCount + 1;
            productJpaRepository.save(Product.create(brandId, "상품이름" + i, Money.of(10*i)));
        }
    }

    @DisplayName("상품목록을 조회할 때,")
    @Nested
    class GetList {
        @DisplayName("상품정보 리스트가 좋아요 수와 함께 정상조회된다")
        @Test
        void succeeds_ProductListWithLikeCounts() {
            // arrange

            // act
            Page<Product> productPage = productService.getProducts(null, PageRequest.of(0, 20), null);

            // assert
            assertAll(
                    () -> assertThat(productPage).isNotNull(),
                    () -> assertThat(productPage.getContent().size()).isEqualTo(20),

                    () -> {
                        Product p = null;
                        for (int i=0;i<20;i++ ) {
                            p = productPage.getContent().get(i);
                            assertThat(p.getName()).isEqualTo("상품이름" + (40-i));
                            assertThat(p.getPrice().getAmount()).isEqualByComparingTo(String.valueOf(10*(40-i)));
                            assertThat(p.getLikeCount()).isEqualTo(0);
                        }
                    }
            );
        }

        @DisplayName("상품을 최신순 으로 정렬할 수 있다.")
        @Test
        void succeeds_whenSortingProductsByLatest() {
            // arrange
            Page<Product> productPage = productService.getProducts(null, PageRequest.of(0, 20), "LATEST");

            // assert
            assertThat(productPage.get())
                    .extracting(BaseEntity::getCreatedAt)
                    .isSortedAccordingTo(Comparator.reverseOrder());
        }

        @DisplayName("특정 브랜드의 상품만 필터링할 수 있다.")
        @Test
        void succeeds_whenFilteringProductsByBrand() {
            // arrange

            // act
            Page<Product> productPage = productService.getProducts(1L, PageRequest.of(0, 10), null);

            // assert
            assertAll(
                    () -> assertThat(productPage).isNotNull(),
                    () -> assertThat(productPage.getContent().size()).isEqualTo(10),

                    () -> {
                        Product p = null;
                        for (int i=0;i<10;i++ ) {
                            p = productPage.getContent().get(i);
                            assertThat(p.getBrandId()).isEqualTo(1L);
                        }
                    }
            );
        }

        @DisplayName("페이징처리가 정상적으로 이루어진다.")
        @Test
        void succeeds_whenPagingProducts() {
            // arrange

            // act
            Page<Product> productPage = productService.getProducts(null, PageRequest.of(1, 20), null);

            // assert
            assertAll(
                    () -> assertThat(productPage).isNotNull(),
                    () -> assertThat(productPage.getContent().size()).isEqualTo(20),

                    () -> {
                        Product p = null;
                        for (int i=0;i<20;i++ ) {
                            p = productPage.getContent().get(i);
                            assertThat(p.getName()).isEqualTo("상품이름" + (20-i));
                        }
                    }
            );
        }
    }

    @DisplayName("상품디테일을 조회할 때,")
    @Nested
    class GetDetail {
        @DisplayName("상품ID가 정상이면 특정 상품정보와 브랜드정보, 좋아요 수가 함께 조회된다.")
        @Test
        void succeeds_whenFetchingProductDetail() {
            // arrange
            Brand brand = Brand.create("나이키", "운동복 브랜드입니다.");
            brandJpaRepository.save(brand);
            Product product = Product.create(brand.getId(), "추가상품1", Money.of("10000"));
            productJpaRepository.save(product);
            stockRepository.save(Stock.create(product.getId(), 1));

            // act
            ProductDetailInfo detail = productFacade.getProductDetail(product.getId());

            // assert
            assertAll(
                    () -> assertThat(detail).isNotNull(),
                    () -> assertThat(detail.brandId()).isEqualTo(brand.getId()),
                    () -> assertThat(detail.brandName()).isEqualTo(brand.getName()),
                    () -> assertThat(detail.productId()).isEqualTo(product.getId()),
                    () -> assertThat(detail.productName()).isEqualTo(product.getName()),
                    () -> assertThat(detail.price()).isEqualByComparingTo(product.getPrice().getAmount()),
                    () -> assertThat(detail.likeCount()).isEqualTo(product.getLikeCount())

            );
        }

        @DisplayName("상품ID가 존재하지 않으면, 조회 실패한다.")
        @Test
        void fails_whenProductIdDoesNotExist() {
            // act + assert
            assertThatThrownBy(() -> productFacade.getProductDetail(-1L))
                    .hasMessageContaining("존재하지 않는 상품입니다.");
        }
    }
}
