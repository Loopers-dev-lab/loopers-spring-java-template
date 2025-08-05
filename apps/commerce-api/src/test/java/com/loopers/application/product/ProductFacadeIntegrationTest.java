package com.loopers.application.product;

import com.loopers.domain.brand.BrandFixture;
import com.loopers.domain.brand.BrandModel;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.product.ProductFixture;
import com.loopers.domain.product.ProductModel;
import com.loopers.domain.product.ProductRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class ProductFacadeIntegrationTest {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ProductFacde productFacade;
    
    private BrandModel savedBrand;
    
    @BeforeEach
    public void setUp() {
        brandRepository.deleteAll();
        productRepository.deleteAll();

        BrandModel brandModel = BrandFixture.createBrandModel();
        savedBrand = brandRepository.save(brandModel);
        
        ProductModel productModel = ProductFixture.createProductWithBrandId(savedBrand.getId());
        productRepository.save(productModel);
        productRepository.save(productModel);
    }

    @Nested
    @DisplayName("brandId가 있는 경우")
    class WithBrandIdTest {

        @DisplayName("특정 브랜드의 상품 목록을 정상적으로 조회한다")
        @Test
        void getProductList_withBrandId_success() {
            // arrange
            ProductCommand.Request.GetList request = new ProductCommand.Request.GetList(savedBrand.getId(), "latest", 0, 10);

            // act
            ProductCommand.ProductData result = productFacade.getProductList(request);

            // assert
            assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.productItemList()).isNotNull(),
                    () -> assertThat(result.productItemList().size()).isEqualTo(2)
            );

        }

        @DisplayName("존재하지 않는 브랜드 ID로 조회 시 예외가 발생한다")
        @Test
        void getProductList_withInvalidBrandId_throwsException() {
            // arrange
            ProductCommand.Request.GetList request = new ProductCommand.Request.GetList(999L, "latest", 0, 10);

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                productFacade.getProductList(request);
            });
            //assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("brandId가 없는 경우")
    class WithoutBrandIdTest {

        @DisplayName("전체 상품 목록을 정상적으로 조회한다")
        @Test
        void getProductList_withoutBrandId_success() {
            // arrange
            ProductCommand.Request.GetList request = new ProductCommand.Request.GetList(null, "latest", 0, 10);

            // act
            ProductCommand.ProductData result = productFacade.getProductList(request);

            // assert
            assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.productItemList()).isNotNull(),
                    () -> assertThat(result.productItemList().size()).isEqualTo(2)
            );
        }

        @DisplayName("상품이 없는 경우 빈 목록을 반환한다")
        @Test
        void getProductList_withoutProducts_returnsEmptyList() {
            // arrange
            productRepository.deleteAll();
            ProductCommand.Request.GetList request = new ProductCommand.Request.GetList(null, "latest", 0, 10);
            // act
            ProductCommand.ProductData result = productFacade.getProductList(request);

            // assert
            assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.productItemList()).isNotNull(),
                    () -> assertThat(result.productItemList().size()).isEqualTo(0)
            );
        }
    }

    @Nested
    @DisplayName("페이징 및 정렬 테스트")
    class PagingAndSortingTest {

        @DisplayName("가격 오름차순 정렬로 조회가 가능하다")
        @Test
        void getProductList_withPriceAscSort() {
            // arrange
            BrandModel brandModel = BrandFixture.createBrandModel();
            BrandModel savedBrand = brandRepository.save(brandModel);
            
            ProductModel productModel = ProductFixture.createProductModel("Product1", savedBrand.getId(), new BigDecimal("100"), new BigDecimal("99999"), "Description1", "url1", "ACTIVE", new BigDecimal("0"));
            productRepository.save(productModel);
            ProductModel productModel1 = ProductFixture.createProductModel("Product2", savedBrand.getId(), new BigDecimal("100"), new BigDecimal("88888"), "Description2", "url2", "ACTIVE", new BigDecimal("0"));
            productRepository.save(productModel1);
            ProductCommand.Request.GetList request = new ProductCommand.Request.GetList(null, "price_asc", 0, 5);

            // act
            ProductCommand.ProductData result = productFacade.getProductList(request);
            int size = result.productItemList().size();
            List<ProductCommand.ProductData.ProductItem> items = result.productItemList();
            // assert
            assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(items).isNotNull(),
                    () -> assertThat(items.get(size - 1).price()).isEqualByComparingTo(new BigDecimal("99999")),
                    () -> assertThat(items.get(size - 2).price()).isEqualByComparingTo(new BigDecimal("88888"))
            );
        }

        @DisplayName("페이지 번호와 사이즈가 올바르게 전달된다")
        @Test
        void getProductList_withCustomPaging() {
            // arrange
            ProductCommand.Request.GetList request = new ProductCommand.Request.GetList(null, "latest", 0, 20);

            // act
            ProductCommand.ProductData result = productFacade.getProductList(request);

            // assert
            assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.productItemList().size()).isEqualTo(2),
                    () -> assertThat(result.productModels().getNumber()).isEqualTo(0)
            );
        }
    }

    @Nested
    @DisplayName("정렬 옵션 테스트")
    class SortingOptionsTest {

        @DisplayName("최신순 정렬이 기본값으로 동작한다")
        @Test
        void getProductList_withLatestSort() {

            // arrange
            productRepository.deleteAll();
            BrandModel brandModel = BrandFixture.createBrandModel();
            BrandModel savedBrand = brandRepository.save(brandModel);
            
            ProductModel productModel = ProductFixture.createProductModel("one", savedBrand.getId(), new BigDecimal("100"), new BigDecimal("10000"), "Description", "url", "ACTIVE", new BigDecimal("0"));
            productRepository.save(productModel);
            ProductModel productModel1 = ProductFixture.createProductModel("two", savedBrand.getId(), new BigDecimal("100"), new BigDecimal("10000"), "Description", "url", "ACTIVE", new BigDecimal("0"));
            productRepository.save(productModel1);
            ProductModel productModel2 = ProductFixture.createProductModel("three", savedBrand.getId(), new BigDecimal("100"), new BigDecimal("10000"), "Description", "url", "ACTIVE", new BigDecimal("0"));
            productRepository.save(productModel2);

            ProductCommand.Request.GetList request = new ProductCommand.Request.GetList(null, "latest", 0, 10);

            // act
            ProductCommand.ProductData result = productFacade.getProductList(request);
            int size = result.productItemList().size();
            List<ProductCommand.ProductData.ProductItem> items = result.productItemList();

            // assert
            assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(items.get(size-1).name()).isEqualTo(productModel.getProductName().getValue()),
                    () -> assertThat(items.get(size-2).name()).isEqualTo(productModel1.getProductName().getValue()),
                    () -> assertThat(items.get(size-3).name()).isEqualTo(productModel2.getProductName().getValue())
            );
        }

        @DisplayName("좋아요 내림차순 정렬이 정상 동작한다")
        @Test
        void getProductList_withLikesDescSort() {
            // arrange
            productRepository.deleteAll();
            BrandModel brandModel = BrandFixture.createBrandModel();
            BrandModel savedBrand = brandRepository.save(brandModel);

            ProductModel productModel = ProductFixture.createProductModel("one", savedBrand.getId(), new BigDecimal("100"), new BigDecimal("10000"), "Description", "url", "ACTIVE", new BigDecimal("0"));
            productModel.incrementLikeCount();
            productRepository.save(productModel);

            ProductModel productModel1 = ProductFixture.createProductModel("two", savedBrand.getId(), new BigDecimal("100"), new BigDecimal("10000"), "Description", "url", "ACTIVE", new BigDecimal("0"));
            productModel1.incrementLikeCount();
            productModel1.incrementLikeCount();
            productModel1.incrementLikeCount();
            productRepository.save(productModel1);

            ProductModel productModel2 = ProductFixture.createProductModel("three", savedBrand.getId(), new BigDecimal("100"), new BigDecimal("10000"), "Description", "url", "ACTIVE", new BigDecimal("0"));
            productModel2.incrementLikeCount();
            productModel2.incrementLikeCount();
            productRepository.save(productModel2);

            ProductCommand.Request.GetList request = new ProductCommand.Request.GetList(null, "likes_desc", 0, 10);

            // act
            ProductCommand.ProductData result = productFacade.getProductList(request);
            int size = result.productItemList().size();
            List<ProductCommand.ProductData.ProductItem> items = result.productItemList();
            // assert
            assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(items.get(size-1).name()).isEqualTo(productModel.getProductName().getValue()),
                    () -> assertThat(items.get(size-2).name()).isEqualTo(productModel2.getProductName().getValue()),
                    () -> assertThat(items.get(size-3).name()).isEqualTo(productModel1.getProductName().getValue())
            );
        }
    }
    @Nested
    @DisplayName("productId로 조회하는 경우")
    class WithProductIdTest {
        @DisplayName("상품이 존재하는 경우 조회가 가능하다")
        @Test
        void getProductListSuccess(){
            // arrange
            BrandModel brandModel = BrandFixture.createBrandModel();
            BrandModel savedBrand = brandRepository.save(brandModel);
            
            ProductModel productModel = ProductFixture.createProductModel("the Rad", savedBrand.getId(), new BigDecimal("100"), new BigDecimal("10000"), "Description", "url", "ACTIVE", new BigDecimal("0"));
            ProductModel saveProduct = productRepository.save(productModel);
            // act
            ProductCommand.ProductData.ProductItem result = productFacade.getProduct(saveProduct.getId());

            // assert
            assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.name()).isEqualTo(saveProduct.getProductName().getValue())
            );
        }
        @DisplayName("상품이 존재하지 않는 경우 조회가 불가능")
        @Test
        void getProductOrThrow_throwsException_whenProductNotFound(){
            // arrange
            Long productId = 100L;
            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                productFacade.getProduct(productId);
            });

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
        @DisplayName("productId가 null인 경우")
        @Test
        void getProductOrThrow_throwsException_whenProductIdIsNull(){
            // arrange
            Long productId = null;
            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                productFacade.getProduct(productId);
            });

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}
