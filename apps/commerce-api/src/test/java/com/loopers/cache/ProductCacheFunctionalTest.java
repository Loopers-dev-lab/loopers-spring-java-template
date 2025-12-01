package com.loopers.cache;

import com.loopers.application.product.ProductFacade;
import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.infrastructure.cache.ProductCacheService;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.interfaces.api.product.ProductDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ProductCacheFunctionalTest {

    @Autowired
    private ProductFacade productFacade;

    @Autowired
    private ProductCacheService productCacheService;

    @Autowired
    private ProductJpaRepository productRepository;

    @Autowired
    private BrandJpaRepository brandRepository;

    private Brand testBrand;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        productCacheService.clearAllProductCache();

        testBrand = brandRepository.save(Brand.create("Test Brand"));
        testProduct = productRepository.save(Product.create(
                "Test Product",
                "Test Description",
                10000L,
                100L,
                testBrand.getId()
        ));
    }

    @DisplayName("캐시 미스 후 조회 시 DB에서 데이터를 가져온다")
    @Test
    void testCacheMissLoadFromDB() {
        ProductDto.ProductListResponse response = productFacade.getProducts(null, "latest", 0, 20);

        assertThat(response.products()).isNotEmpty();
        assertThat(response.products()).anyMatch(p -> p.id().equals(testProduct.getId()));
    }

    @DisplayName("캐시 히트 시 캐시에서 데이터를 반환한다")
    @Test
    void testCacheHitReturnsCachedData() {
        productFacade.getProducts(null, "latest", 0, 20);
        ProductDto.ProductListResponse cachedResponse = productFacade.getProducts(null, "latest", 0, 20);

        assertThat(cachedResponse.products()).isNotEmpty();
        assertThat(cachedResponse.products()).anyMatch(p -> p.id().equals(testProduct.getId()));
    }

    @DisplayName("상품 상세 조회가 캐시와 함께 작동한다")
    @Test
    void testDetailCacheFunctions() {
        ProductDto.ProductDetailResponse firstCall = productFacade.getProduct(testProduct.getId());
        assertThat(firstCall).isNotNull();
        assertThat(firstCall.id()).isEqualTo(testProduct.getId());

        ProductDto.ProductDetailResponse secondCall = productFacade.getProduct(testProduct.getId());
        assertThat(secondCall).isNotNull();
        assertThat(secondCall.id()).isEqualTo(firstCall.id());
        assertThat(secondCall.name()).isEqualTo(firstCall.name());
    }

    @DisplayName("브랜드 필터링이 캐시와 함께 작동한다")
    @Test
    void testListCacheWithBrandFilter() {
        Brand anotherBrand = brandRepository.save(Brand.create("Another Brand"));
        Product anotherProduct = productRepository.save(Product.create(
                "Another Product",
                "Another Description",
                20000L,
                50L,
                anotherBrand.getId()
        ));

        ProductDto.ProductListResponse filtered = productFacade.getProducts(testBrand.getId(), "latest", 0, 20);

        assertThat(filtered.products())
                .anyMatch(p -> p.id().equals(testProduct.getId()))
                .noneMatch(p -> p.id().equals(anotherProduct.getId()));
    }

    @DisplayName("전체 응답 캐싱으로 일관된 데이터를 반환한다")
    @Test
    void testFullResponseCaching() {
        ProductDto.ProductListResponse first = productFacade.getProducts(null, "latest", 0, 20);
        ProductDto.ProductListResponse second = productFacade.getProducts(null, "latest", 0, 20);

        assertThat(first.totalCount()).isEqualTo(second.totalCount());
        assertThat(first.products()).hasSameSizeAs(second.products());
    }
}
