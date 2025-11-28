package com.loopers.domain.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.common.vo.Price;
import com.loopers.domain.metrics.product.ProductMetrics;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
@DisplayName("상품 서비스(ProductService) 테스트")
public class ProductServiceIntegrationTest {

    @MockitoSpyBean
    private ProductRepository spyProductRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private com.loopers.infrastructure.brand.BrandJpaRepository brandJpaRepository;

    @Autowired
    private com.loopers.infrastructure.product.ProductJpaRepository productJpaRepository;

    @Autowired
    private com.loopers.infrastructure.metrics.product.ProductMetricsJpaRepository productMetricsJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private Long brandId;
    private Long productId1;
    private Long productId2;
    private Long productId3;

    @BeforeEach
    void setup() {
        // Brand 등록
        Brand brand = Brand.create("Nike");
        Brand savedBrand = brandJpaRepository.save(brand);
        brandId = savedBrand.getId();

        // Product 등록
        Product product1 = Product.create("상품1", brandId, new Price(10000));
        Product savedProduct1 = productJpaRepository.save(product1);
        productId1 = savedProduct1.getId();
        // ProductMetrics 등록
        ProductMetrics metrics1 = ProductMetrics.create(productId1, 4);
        productMetricsJpaRepository.save(metrics1);

        Product product2 = Product.create("상품2", brandId, new Price(20000));
        Product savedProduct2 = productJpaRepository.save(product2);
        productId2 = savedProduct2.getId();
        // ProductMetrics 등록
        ProductMetrics metrics2 = ProductMetrics.create(productId2, 0);
        productMetricsJpaRepository.save(metrics2);

        Product product3 = Product.create("상품3", brandId, new Price(15000));
        Product savedProduct3 = productJpaRepository.save(product3);
        productId3 = savedProduct3.getId();
        // ProductMetrics 등록
        ProductMetrics metrics3 = ProductMetrics.create(productId3, 3);
        productMetricsJpaRepository.save(metrics3);
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("상품 ID로 상품을 조회할 때, ")
    @Nested
    class GetProductById {
        @DisplayName("존재하는 상품 ID로 조회하면 상품을 반환한다. (Happy Path)")
        @Test
        void should_returnProduct_when_productExists() {
            // arrange
            Long productId = 1L;

            // act
            Product result = productService.getProductById(productId);

            // assert
            verify(spyProductRepository).findById(1L);
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("상품1");
        }

        @DisplayName("존재하지 않는 상품 ID로 조회하면 예외가 발생한다. (Exception)")
        @Test
        void should_throwException_when_productNotFound() {
            // arrange
            Long productId = 999L;
            when(spyProductRepository.findById(productId)).thenReturn(Optional.empty());

            // act
            CoreException exception = assertThrows(CoreException.class, () -> {
                productService.getProductById(productId);
            });

            // assert
            verify(spyProductRepository).findById(999L);
            assertThat(exception.getMessage()).isEqualTo("상품을 찾을 수 없습니다.");
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }
    }

    @DisplayName("여러 상품 ID로 상품 맵을 조회할 때, ")
    @Nested
    class GetProductMapByIds {
        @DisplayName("존재하는 상품 ID들로 조회하면 상품 맵을 반환한다. (Happy Path)")
        @Test
        void should_returnProductMap_when_productsExist() {
            // arrange
            List<Long> productIds = List.of(1L, 2L, 3L);

            // act
            Map<Long, Product> result = productService.getProductMapByIds(productIds);

            // assert
            verify(spyProductRepository).findAllByIdIn(any(Collection.class));
            assertThat(result).hasSize(3);
            assertThat(result.get(productId1).getName()).isEqualTo("상품1");
            assertThat(result.get(productId2).getName()).isEqualTo("상품2");
            assertThat(result.get(productId3).getName()).isEqualTo("상품3");
        }

        @DisplayName("빈 ID 리스트로 조회하면 빈 맵을 반환한다. (Edge Case)")
        @Test
        void should_returnEmptyMap_when_emptyIdList() {
            // arrange
            List<Long> productIds = Collections.emptyList();
            when(spyProductRepository.findAllByIdIn(productIds)).thenReturn(Collections.emptyList());

            // act
            Map<Long, Product> result = productService.getProductMapByIds(productIds);

            // assert
            verify(spyProductRepository).findAllByIdIn(any(Collection.class));
            assertThat(result).isEmpty();
        }

        @DisplayName("존재하지 않는 상품 ID들로 조회하면 빈 맵을 반환한다. (Edge Case)")
        @Test
        void should_returnEmptyMap_when_productsNotFound() {
            // arrange
            List<Long> productIds = List.of(999L, 1000L);
            when(spyProductRepository.findAllByIdIn(productIds)).thenReturn(Collections.emptyList());

            // act
            Map<Long, Product> result = productService.getProductMapByIds(productIds);

            // assert
            verify(spyProductRepository).findAllByIdIn(any(Collection.class));
            assertThat(result).isEmpty();
        }
    }

    @DisplayName("상품 목록을 조회할 때, ")
    @Nested
    class GetProducts {
        @DisplayName("기본 페이지네이션으로 조회하면 상품 페이지를 반환한다. (Happy Path)")
        @Test
        void should_returnProductPage_when_defaultPageable() {
            // arrange
            Pageable pageable = PageRequest.of(0, 20);

            // act
            Page<Product> result = productService.getProducts(pageable);

            // assert
            verify(spyProductRepository).findAll(any(Pageable.class));
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(3);
            assertThat(result.getTotalElements()).isEqualTo(3);
        }

        @DisplayName("최신순 정렬로 조회하면 상품 페이지를 반환한다. (Happy Path)")
        @Test
        void should_returnProductPage_when_sortedByLatest() {
            // arrange
            Sort sort = Sort.by("latest");
            Pageable pageable = PageRequest.of(0, 20, sort);

            // act
            Page<Product> result = productService.getProducts(pageable);

            // assert
            verify(spyProductRepository).findAll(any(Pageable.class));
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(3);
            // 최신순 정렬 검증 createdAt 기준 내림차순
            assertThat(result.getContent().get(0).getCreatedAt()).isAfterOrEqualTo(result.getContent().get(1).getCreatedAt());
            assertThat(result.getContent().get(1).getCreatedAt()).isAfterOrEqualTo(result.getContent().get(2).getCreatedAt());
        }

        @DisplayName("가격 오름차순 정렬로 조회하면 상품 페이지를 반환한다. (Happy Path)")
        @Test
        void should_returnProductPage_when_sortedByPriceAsc() {
            // arrange
            Sort sort = Sort.by("price_asc");
            Pageable pageable = PageRequest.of(0, 20, sort);

            // act
            Page<Product> result = productService.getProducts(pageable);

            // assert
            verify(spyProductRepository).findAll(any(Pageable.class));
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(3);
            // 가격 오름차순 정렬 검증
            assertThat(result.getContent().get(0).getPrice().amount()).isLessThanOrEqualTo(result.getContent().get(1).getPrice().amount());
            assertThat(result.getContent().get(1).getPrice().amount()).isLessThanOrEqualTo(result.getContent().get(2).getPrice().amount());
        }

        @DisplayName("좋아요 내림차순 정렬로 조회하면 상품 페이지를 반환한다. (Happy Path)")
        @Test
        void should_returnProductPage_when_sortedByLikesDesc() {
            // arrange
            Sort sort = Sort.by("likes_desc");
            Pageable pageable = PageRequest.of(0, 20, sort);

            // act
            Page<Product> result = productService.getProducts(pageable);

            // assert
            verify(spyProductRepository).findAll(any(Pageable.class));
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(3);
            // 좋아요 내림차순 정렬 검증
            assertThat(result.getContent().get(0).getId()).isGreaterThanOrEqualTo(result.getContent().get(1).getId());
            assertThat(result.getContent().get(1).getId()).isGreaterThanOrEqualTo(result.getContent().get(2).getId());
        }
    }

}
