package com.loopers.domain.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class ProductServiceIntegrationTest {

    @Autowired
    private ProductService productService;

    @MockitoSpyBean
    private ProductRepository productRepository;

    @MockitoSpyBean
    private BrandRepository brandRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private Brand savedBrand1;
    private Brand savedBrand2;
    private Product savedProduct1;
    private Product savedProduct2;
    private Product savedProduct3;

    @BeforeEach
    void setUp() {
        savedBrand1 = brandRepository.save(Brand.create("Brand A"));
        savedBrand2 = brandRepository.save(Brand.create("Brand B"));

        // Brand A 소속 상품 2개
        savedProduct1 = productRepository.save(Product.create("Product 1 (A)", 1000L, 10, savedBrand1));
        savedProduct2 = productRepository.save(Product.create("Product 2 (A)", 2000L, 20, savedBrand1));

        // Brand B 소속 상품 1개
        savedProduct3 = productRepository.save(Product.create("Product 3 (B)", 3000L, 30, savedBrand2));
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("ID로 상품 단건 조회를 할 수 있다.")
    @Test
    void getProduct() {
        // act
        Product foundProduct = productService.getProduct(savedProduct1.getId());

        // assert
        assertAll(
                () -> assertThat(foundProduct).isNotNull(),
                () -> assertThat(foundProduct.getId()).isEqualTo(savedProduct1.getId()),
                () -> assertThat(foundProduct.getName()).isEqualTo("Product 1 (A)")
        );
    }

    @DisplayName("존재하지 않는 ID로 상품 조회 시, NOT_FOUND 예외가 발생한다.")
    @Test
    void getProduct_throwsNotFound() {
        // act & assert
        CoreException exception = assertThrows(CoreException.class, () -> {
            productService.getProduct(-1L);
        });

        assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
    }

    @DisplayName("ID 목록으로 여러 상품을 조회할 수 있다.")
    @Test
    void getProductsByIds() {
        // act
        List<Product> products = productService.getProductsByIds(
                List.of(savedProduct1.getId(), savedProduct3.getId())
        );

        // assert
        assertThat(products).hasSize(2);
        assertThat(products).extracting(Product::getName)
                .containsExactlyInAnyOrder("Product 1 (A)", "Product 3 (B)");
    }

    @DisplayName("ID 목록 중 존재하지 않는 ID가 포함되면, NOT_FOUND 예외가 발생한다.")
    @Test
    void getProductsByIds_throwsNotFound() {
        // act & assert
        CoreException exception = assertThrows(CoreException.class, () -> {
            productService.getProductsByIds(List.of(savedProduct1.getId(), -1L));
        });

        assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
    }

    @DisplayName("상품 목록을 페이징하여 조회할 수 있다 (브랜드 ID 필터링).")
    @Test
    void getProducts_withPagingAndBrandFilter() {
        // arrange
        Pageable pageable = PageRequest.of(0, 1, Sort.by("id").ascending());

        // act
        ProductSearchCondition productSearchCondition = new ProductSearchCondition(savedBrand1.getId(), pageable);
        Page<Product> productPage = productService.getProducts(productSearchCondition);

        // assert
        assertAll(
                () -> assertThat(productPage.getTotalElements()).isEqualTo(2),
                () -> assertThat(productPage.getTotalPages()).isEqualTo(2),
                () -> assertThat(productPage.getContent()).hasSize(1),
                () -> assertThat(productPage.getContent().get(0).getName()).isEqualTo("Product 1 (A)")
        );
    }

    @DisplayName("상품 목록을 페이징하여 조회할 수 있다 (필터링 없음).")
    @Test
    void getProducts_withPaging() {
        // arrange
        Pageable pageable = PageRequest.of(0, 2, Sort.by("id").ascending());

        // act
        ProductSearchCondition productSearchCondition = new ProductSearchCondition(null, pageable);
        Page<Product> productPage = productService.getProducts(productSearchCondition);

        // assert
        assertAll(
                () -> assertThat(productPage.getTotalElements()).isEqualTo(3),
                () -> assertThat(productPage.getTotalPages()).isEqualTo(2),
                () -> assertThat(productPage.getContent()).hasSize(2),
                () -> assertThat(productPage.getContent().get(0).getName()).isEqualTo("Product 1 (A)"),
                () -> assertThat(productPage.getContent().get(1).getName()).isEqualTo("Product 2 (A)")
        );
    }
}
