package com.loopers.application.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.loopers.domain.brand.BrandService;
import com.loopers.domain.product.LikeCount;
import com.loopers.domain.product.Price;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.product.Stock;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
class ProductFacadeIntegrationTest {

    @Autowired
    private ProductFacade productFacade;

    @MockitoSpyBean
    private ProductService productService;

    @MockitoSpyBean
    private BrandService brandService;

    @DisplayName("상품 목록을 조회할 때,")
    @Nested
    class GetProducts {

        @DisplayName("LATEST 정렬로 조회하면 최신순 상품 목록이 반환된다.")
        @Test
        void returnsProducts_whenSortedByLatest() {
            // arrange
            Long brandId = null;
            int page = 0;
            int size = 20;
            ProductCommand.GetProductsCommand command = new ProductCommand.GetProductsCommand(
                    brandId, ProductSort.LATEST, page, size
            );

            Product product1 = createProduct(1L, "상품1", 1L, 10000, 10, 100);
            Product product2 = createProduct(2L, "상품2", 1L, 20000, 20, 200);
            List<Product> products = List.of(product1, product2);

            Map<Long, String> brandNamesMap = new HashMap<>();
            brandNamesMap.put(1L, "브랜드1");

            doReturn(products).when(productService).findProductsByLatestWithBrandName(brandId, page, size);
            doReturn(brandNamesMap).when(brandService).findBrandNamesByIds(any());

            // act
            List<ProductInfo> result = productFacade.getProducts(command);

            // assert
            assertThat(result).hasSize(2);
            assertAll(
                    () -> assertThat(result.get(0).id()).isEqualTo(1L),
                    () -> assertThat(result.get(0).name()).isEqualTo("상품1"),
                    () -> assertThat(result.get(0).brandName()).isEqualTo("브랜드1"),
                    () -> assertThat(result.get(1).id()).isEqualTo(2L),
                    () -> assertThat(result.get(1).name()).isEqualTo("상품2"),
                    () -> assertThat(result.get(1).brandName()).isEqualTo("브랜드1")
            );

            // verify
            verify(productService, times(1)).findProductsByLatestWithBrandName(brandId, page, size);
            verify(brandService, times(1)).findBrandNamesByIds(any());
        }

        @DisplayName("PRICE_ASC 정렬로 조회하면 가격 오름차순 상품 목록이 반환된다.")
        @Test
        void returnsProducts_whenSortedByPriceAsc() {
            // arrange
            Long brandId = null;
            int page = 0;
            int size = 20;
            ProductCommand.GetProductsCommand command = new ProductCommand.GetProductsCommand(
                    brandId, ProductSort.PRICE_ASC, page, size
            );

            Product product1 = createProduct(1L, "상품1", 1L, 10000, 10, 100);
            Product product2 = createProduct(2L, "상품2", 2L, 20000, 20, 200);
            List<Product> products = List.of(product1, product2);

            Map<Long, String> brandNamesMap = new HashMap<>();
            brandNamesMap.put(1L, "브랜드1");
            brandNamesMap.put(2L, "브랜드2");

            doReturn(products).when(productService).findProductsByPriceAscWithBrandName(brandId, page, size);
            doReturn(brandNamesMap).when(brandService).findBrandNamesByIds(any());

            // act
            List<ProductInfo> result = productFacade.getProducts(command);

            // assert
            assertThat(result).hasSize(2);
            assertAll(
                    () -> assertThat(result.get(0).id()).isEqualTo(1L),
                    () -> assertThat(result.get(0).brandName()).isEqualTo("브랜드1"),
                    () -> assertThat(result.get(1).id()).isEqualTo(2L),
                    () -> assertThat(result.get(1).brandName()).isEqualTo("브랜드2")
            );

            // verify
            verify(productService, times(1)).findProductsByPriceAscWithBrandName(brandId, page, size);
            verify(brandService, times(1)).findBrandNamesByIds(any());
        }

        @DisplayName("LIKES_DESC 정렬로 조회하면 좋아요 내림차순 상품 목록이 반환된다.")
        @Test
        void returnsProducts_whenSortedByLikesDesc() {
            // arrange
            Long brandId = null;
            int page = 0;
            int size = 20;
            ProductCommand.GetProductsCommand command = new ProductCommand.GetProductsCommand(
                    brandId, ProductSort.LIKES_DESC, page, size
            );

            Product product1 = createProduct(1L, "상품1", 1L, 10000, 100, 100);
            Product product2 = createProduct(2L, "상품2", 1L, 20000, 50, 200);
            List<Product> products = List.of(product1, product2);

            Map<Long, String> brandNamesMap = new HashMap<>();
            brandNamesMap.put(1L, "브랜드1");

            doReturn(products).when(productService).findProductsByLikesDescWithBrandName(brandId, page, size);
            doReturn(brandNamesMap).when(brandService).findBrandNamesByIds(any());

            // act
            List<ProductInfo> result = productFacade.getProducts(command);

            // assert
            assertThat(result).hasSize(2);
            assertAll(
                    () -> assertThat(result.get(0).id()).isEqualTo(1L),
                    () -> assertThat(result.get(0).likeCount()).isEqualTo(100),
                    () -> assertThat(result.get(1).id()).isEqualTo(2L),
                    () -> assertThat(result.get(1).likeCount()).isEqualTo(50)
            );

            // verify
            verify(productService, times(1)).findProductsByLikesDescWithBrandName(brandId, page, size);
            verify(brandService, times(1)).findBrandNamesByIds(any());
        }

        @DisplayName("여러 brandId가 있는 경우 중복을 제거하고 한 번에 조회한다.")
        @Test
        void deduplicatesBrandIds_whenMultipleProductsHaveSameBrand() {
            // arrange
            Long brandId = null;
            int page = 0;
            int size = 20;
            ProductCommand.GetProductsCommand command = new ProductCommand.GetProductsCommand(
                    brandId, ProductSort.LATEST, page, size
            );

            Product product1 = createProduct(1L, "상품1", 1L, 10000, 10, 100);
            Product product2 = createProduct(2L, "상품2", 1L, 20000, 20, 200);
            Product product3 = createProduct(3L, "상품3", 2L, 30000, 30, 300);
            List<Product> products = List.of(product1, product2, product3);

            Map<Long, String> brandNamesMap = new HashMap<>();
            brandNamesMap.put(1L, "브랜드1");
            brandNamesMap.put(2L, "브랜드2");

            doReturn(products).when(productService).findProductsByLatestWithBrandName(brandId, page, size);
            doReturn(brandNamesMap).when(brandService).findBrandNamesByIds(any());

            // act
            List<ProductInfo> result = productFacade.getProducts(command);

            // assert
            assertThat(result).hasSize(3);
            assertAll(
                    () -> assertThat(result.get(0).brandName()).isEqualTo("브랜드1"),
                    () -> assertThat(result.get(1).brandName()).isEqualTo("브랜드1"),
                    () -> assertThat(result.get(2).brandName()).isEqualTo("브랜드2")
            );

            // verify
            verify(productService, times(1)).findProductsByLatestWithBrandName(brandId, page, size);
            verify(brandService, times(1)).findBrandNamesByIds(any());
        }

        @DisplayName("brandId로 필터링하면 해당 브랜드의 상품만 조회된다.")
        @Test
        void returnsProducts_whenFilteredByBrandId() {
            // arrange
            Long brandId = 1L;
            int page = 0;
            int size = 20;
            ProductCommand.GetProductsCommand command = new ProductCommand.GetProductsCommand(
                    brandId, ProductSort.LATEST, page, size
            );

            Product product1 = createProduct(1L, "상품1", 1L, 10000, 10, 100);
            List<Product> products = List.of(product1);

            Map<Long, String> brandNamesMap = new HashMap<>();
            brandNamesMap.put(1L, "브랜드1");

            doReturn(products).when(productService).findProductsByLatestWithBrandName(brandId, page, size);
            doReturn(brandNamesMap).when(brandService).findBrandNamesByIds(any());

            // act
            List<ProductInfo> result = productFacade.getProducts(command);

            // assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).brandId()).isEqualTo(1L);
            assertThat(result.get(0).brandName()).isEqualTo("브랜드1");

            // verify
            verify(productService, times(1)).findProductsByLatestWithBrandName(brandId, page, size);
            verify(brandService, times(1)).findBrandNamesByIds(any());
        }

        @DisplayName("상품이 없으면 빈 리스트가 반환된다.")
        @Test
        void returnsEmptyList_whenNoProductsExist() {
            // arrange
            Long brandId = null;
            int page = 0;
            int size = 20;
            ProductCommand.GetProductsCommand command = new ProductCommand.GetProductsCommand(
                    brandId, ProductSort.LATEST, page, size
            );

            List<Product> products = List.of();

            doReturn(products).when(productService).findProductsByLatestWithBrandName(brandId, page, size);
            doReturn(new HashMap<>()).when(brandService).findBrandNamesByIds(any());

            // act
            List<ProductInfo> result = productFacade.getProducts(command);

            // assert
            assertThat(result).isEmpty();

            // verify
            verify(productService, times(1)).findProductsByLatestWithBrandName(brandId, page, size);
            verify(brandService, times(1)).findBrandNamesByIds(any());
        }
    }

    @DisplayName("상품 정보를 조회할 때,")
    @Nested
    class GetProduct {

        @DisplayName("상품이 존재하면 상품 정보가 반환된다.")
        @Test
        void returnsProductInfo_whenProductExists() {
            // arrange
            Long productId = 1L;
            Product product = createProduct(1L, "상품1", 1L, 10000, 10, 100);
            String brandName = "브랜드1";

            doReturn(Optional.of(product)).when(productService).findProductById(productId);
            doReturn(brandName).when(brandService).findBrandNameById(1L);

            // act
            ProductInfo result = productFacade.getProduct(productId);

            // assert
            assertAll(
                    () -> assertThat(result.id()).isEqualTo(1L),
                    () -> assertThat(result.name()).isEqualTo("상품1"),
                    () -> assertThat(result.brandId()).isEqualTo(1L),
                    () -> assertThat(result.brandName()).isEqualTo("브랜드1"),
                    () -> assertThat(result.price()).isEqualTo(10000),
                    () -> assertThat(result.likeCount()).isEqualTo(10),
                    () -> assertThat(result.stock()).isEqualTo(100)
            );

            // verify
            verify(productService, times(1)).findProductById(productId);
            verify(brandService, times(1)).findBrandNameById(1L);
        }

        @DisplayName("상품이 존재하지 않으면 NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsException_whenProductDoesNotExist() {
            // arrange
            Long productId = 999L;

            doReturn(Optional.empty()).when(productService).findProductById(productId);

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                productFacade.getProduct(productId);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
            assertThat(exception.getMessage()).contains("상품을 찾을 수 없습니다.");

            // verify
            verify(productService, times(1)).findProductById(productId);
            verify(brandService, never()).findBrandNameById(anyLong());
        }

        @DisplayName("브랜드가 존재하지 않으면 NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsException_whenBrandDoesNotExist() {
            // arrange
            Long productId = 1L;
            Product product = createProduct(1L, "상품1", 999L, 10000, 10, 100);

            doReturn(Optional.of(product)).when(productService).findProductById(productId);
            doThrow(new CoreException(ErrorType.NOT_FOUND, "브랜드를 찾을 수 없습니다."))
                    .when(brandService).findBrandNameById(999L);

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                productFacade.getProduct(productId);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
            assertThat(exception.getMessage()).contains("브랜드를 찾을 수 없습니다.");

            // verify
            verify(productService, times(1)).findProductById(productId);
            verify(brandService, times(1)).findBrandNameById(999L);
        }
    }

    private Product createProduct(Long id, String name, Long brandId, Integer price, Integer likeCount, Integer stock) {
        Product product = mock(Product.class);
        Price priceValue = mock(Price.class);
        LikeCount likeCountValue = mock(LikeCount.class);
        Stock stockValue = mock(Stock.class);

        when(product.getId()).thenReturn(id);
        when(product.getName()).thenReturn(name);
        when(product.getBrandId()).thenReturn(brandId);
        when(product.getPrice()).thenReturn(priceValue);
        when(product.getLikeCount()).thenReturn(likeCountValue);
        when(product.getStock()).thenReturn(stockValue);
        when(priceValue.getPrice()).thenReturn(price);
        when(likeCountValue.getCount()).thenReturn(likeCount);
        when(stockValue.getQuantity()).thenReturn(stock);

        return product;
    }
}

