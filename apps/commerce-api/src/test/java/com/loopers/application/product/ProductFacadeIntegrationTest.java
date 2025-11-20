package com.loopers.application.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductSortType;
import com.loopers.domain.product.vo.Stock;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.utils.DatabaseCleanUp;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ProductFacadeIntegrationTest {
    @Autowired
    private ProductFacade productFacade;
    @Autowired
    private ProductJpaRepository productJpaRepository;
    @Autowired
    private BrandJpaRepository brandJpaRepository;
    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("상품을 조회할 때,")
    @Nested
    class Get {

        @Test
        void 상품_상세_조회시_상품_PK_ID를_주면_상품_정보와_브랜드_좋아요수가_포함된다() {
            // arrange
            String brandName = "brandTest";
            Brand brand = brandJpaRepository.save(Brand.create(brandName));
            String productName = "productTest";
            Integer price = 10000;
            Stock stock = new Stock(10);
            Product product = productJpaRepository.save(Product.create(productName, price, stock, brand.getId()));

            // act
            ProductWithBrandInfo sut = productFacade.getProductDetail(product.getId());

            // assert
            assertAll(
                    () -> assertThat(sut.id()).isEqualTo(product.getId()),
                    () -> assertThat(sut.brandId()).isEqualTo(brand.getId())
            );
        }

        @Test
        void 상품_리스트_조회시_최근순_정렬() {
            // arrange
            Brand brand = brandJpaRepository.save(Brand.create("brandTest"));
            productJpaRepository.save(Product.create("testA", 1000, new Stock(2), brand.getId()));
            productJpaRepository.save(Product.create("testB", 2000, new Stock(2), brand.getId()));
            ProductSortType productSortType = ProductSortType.LATEST;

            // act
            List<ProductWithBrandInfo> products = productFacade.getProductList(productSortType);

            assertThat(products).hasSize(2)
                    .extracting(ProductWithBrandInfo::name)
                    .containsExactly("testB", "testA");
        }

        @Test
        void 상품_리스트_조회시_가격_낮은순_정렬() {
            // arrange
            Brand brand = brandJpaRepository.save(Brand.create("brandTest"));
            productJpaRepository.save(Product.create("testA", 1000, new Stock(2), brand.getId()));
            productJpaRepository.save(Product.create("testB", 2000, new Stock(2), brand.getId()));
            ProductSortType productSortType = ProductSortType.PRICE_ASC;

            // act
            List<ProductWithBrandInfo> products = productFacade.getProductList(productSortType);

            assertThat(products).hasSize(2)
                    .extracting(ProductWithBrandInfo::price)
                    .containsExactly(1000, 2000);
        }

        @Test
        void 상품_리스트_조회시_좋아요_많은순_정렬() {
            // arrange
            Brand brand = brandJpaRepository.save(Brand.create("brandTest"));
            productJpaRepository.save(Product.create("testA", 1000, new Stock(2), brand.getId()));
            Product testB = Product.create("testB", 2000, new Stock(2), brand.getId());
            testB.likeCountIncrease(30);
            productJpaRepository.save(testB);
            ProductSortType productSortType = ProductSortType.LIKES_DESC;

            // act
            List<ProductWithBrandInfo> products = productFacade.getProductList(productSortType);

            assertThat(products).hasSize(2)
                    .extracting(ProductWithBrandInfo::name)
                    .containsExactly("testB", "testA");
        }

    }
}
