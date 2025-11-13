package com.loopers.domain.product;

import com.loopers.domain.brand.Brand;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ActiveProfiles("test")
@SpringBootTest
class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private EntityManager entityManager;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("상품 코드, 상품명, 가격, 수량, 브랜드를 입력받아, 상품을 등록한다.")
    @Test
    @Transactional
    void whenRegisterProduct_thenSuccess() {

        // given
        Brand brand = Brand.createBrand("리바이스");
        entityManager.persist(brand);

        String productCode = "P001";
        String productName = "청바지";
        BigDecimal price = BigDecimal.valueOf(25000);
        int stock = 200;

        // when
        Product productResponse = productService.registerProduct(
                        productCode, productName, price, stock, brand
                );

        // then
        assertAll(
                () -> assertThat(productResponse.getProductCode()).isEqualTo("P001"),
                () -> assertThat(productResponse.getProductName()).isEqualTo("청바지"),
                () -> assertThat(productResponse.getPrice()).isEqualTo(BigDecimal.valueOf(25000)),
                () -> assertThat(productResponse.getStock()).isEqualTo(200),
                () -> assertThat(productResponse.getBrand()).isNotNull(),
                () -> assertThat(productResponse.getBrand().getBrandName()).isEqualTo("리바이스")
        );
    }

    @DisplayName("이미 존재하는 상품 코드는 등록에 실패한다.")
    @Test
    @Transactional
    void whenRegisterProductWithDuplicateCode_thenBadRequest() {

        // given
        Brand brand = Brand.createBrand("리바이스");
        entityManager.persist(brand);

        String productCode1 = "P001";
        String productName1 = "청바지";
        BigDecimal price1 = BigDecimal.valueOf(25000);
        int stock1 = 200;

        String productCode2 = "P001";
        String productName2 = "청바지";
        BigDecimal price2 = BigDecimal.valueOf(25000);
        int stock2 = 100;

        // when
        productService.registerProduct(productCode1, productName1, price1, stock1, brand);

        // then
        CoreException result = assertThrows(CoreException.class, () -> {
            productService.registerProduct(productCode2, productName2, price2, stock2, brand);
        });

        assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(result.getCustomMessage()).isEqualTo("중복된 상품 코드 오류");
    }

    @DisplayName("정렬 조건이 null인 경우, 기본값(최신순)으로 상품 목록을 조회한다.")
    @Test
    @Transactional
    void whenGetProductsWithNullSortType_thenReturnLatestOrder() {
        // given
        Brand brand = Brand.createBrand("테스트브랜드");
        entityManager.persist(brand);

        productService.registerProduct("P001", "상품1", BigDecimal.valueOf(10000), 10, brand);
        productService.registerProduct("P002", "상품2", BigDecimal.valueOf(20000), 20, brand);
        productService.registerProduct("P003", "상품3", BigDecimal.valueOf(15000), 15, brand);

        // when
        List<Product> products = productService.getProducts(null);

        // then
        assertThat(products).isNotEmpty();
        assertThat(products.size()).isEqualTo(3);
    }

    @DisplayName("LATEST 정렬 조건으로 상품 목록을 최신순으로 조회한다.")
    @Test
    @Transactional
    void whenGetProductsWithLatest_thenReturnLatestOrder() {
        // given
        Brand brand = Brand.createBrand("테스트브랜드");
        entityManager.persist(brand);

        productService.registerProduct("P001", "상품1", BigDecimal.valueOf(10000), 10, brand);
        productService.registerProduct("P002", "상품2", BigDecimal.valueOf(20000), 20, brand);
        productService.registerProduct("P003", "상품3", BigDecimal.valueOf(15000), 15, brand);

        // when
        List<Product> products = productService.getProducts(ProductSortType.LATEST);

        // then
        assertThat(products).isNotEmpty();
        assertThat(products).hasSize(3);
        // 최신순이므로 P003, P002, P001 순서
        assertThat(products.get(0).getProductCode()).isEqualTo("P003");
        assertThat(products.get(1).getProductCode()).isEqualTo("P002");
        assertThat(products.get(2).getProductCode()).isEqualTo("P001");
    }

    @DisplayName("PRICE_ASC 정렬 조건으로 상품 목록을 가격 낮은 순으로 조회한다.")
    @Test
    @Transactional
    void whenGetProductsWithPriceAsc_thenReturnPriceAscOrder() {
        // given
        Brand brand = Brand.createBrand("테스트브랜드");
        entityManager.persist(brand);

        productService.registerProduct("P001", "상품1", BigDecimal.valueOf(30000), 10, brand);
        productService.registerProduct("P002", "상품2", BigDecimal.valueOf(10000), 20, brand);
        productService.registerProduct("P003", "상품3", BigDecimal.valueOf(20000), 15, brand);

        // when
        List<Product> products = productService.getProducts(ProductSortType.PRICE_ASC);

        // then
        assertThat(products).isNotEmpty();
        assertThat(products.size()).isEqualTo(3);
        // 가격 낮은 순이므로 P002(10000), P003(20000), P001(30000) 순서
        assertThat(products.get(0).getProductCode()).isEqualTo("P002");
        assertThat(products.get(0).getPrice()).isEqualByComparingTo(BigDecimal.valueOf(10000));
        assertThat(products.get(1).getProductCode()).isEqualTo("P003");
        assertThat(products.get(1).getPrice()).isEqualByComparingTo(BigDecimal.valueOf(20000));
        assertThat(products.get(2).getProductCode()).isEqualTo("P001");
        assertThat(products.get(2).getPrice()).isEqualByComparingTo(BigDecimal.valueOf(30000));
    }

    @DisplayName("LIKES_DESC 정렬 조건으로 상품 목록을 좋아요 많은 순으로 조회한다.")
    @Test
    @Transactional
    void whenGetProductsWithLikesDesc_thenReturnLikesDescOrder() {
        // given
        Brand brand = Brand.createBrand("테스트브랜드");
        entityManager.persist(brand);

        productService.registerProduct("P001", "상품1", BigDecimal.valueOf(10000), 10, brand);
        productService.registerProduct("P002", "상품2", BigDecimal.valueOf(20000), 20, brand);
        productService.registerProduct("P003", "상품3", BigDecimal.valueOf(15000), 15, brand);

        // when
        List<Product> products = productService.getProducts(ProductSortType.LIKES_DESC);

        // then
        assertThat(products).isNotEmpty();
        assertThat(products.size()).isEqualTo(3);
        // 좋아요 많은 순 (초기 좋아요는 모두 0이므로 조회만 확인)
    }

    @DisplayName("상품 목록 조회 시 삭제된 상품은 제외된다.")
    @Test
    @Transactional
    void whenGetProducts_thenExcludeDeletedProducts() {
        // given
        Brand brand = Brand.createBrand("테스트브랜드");
        entityManager.persist(brand);

        Product product1 = productService.registerProduct("P001", "상품1", BigDecimal.valueOf(10000), 10, brand);
        productService.registerProduct("P002", "상품2", BigDecimal.valueOf(20000), 20, brand);
        productService.registerProduct("P003", "상품3", BigDecimal.valueOf(15000), 15, brand);

        // 상품1 삭제
        product1.delete();

        // DB에 삭제 상태 반영 및 영속성 컨텍스트 초기화
        entityManager.flush();
        entityManager.clear();

        // when
        List<Product> products = productService.getProducts(ProductSortType.LATEST);

        // then
        // 삭제되지 않은 상품만 조회되어야 함
        assertThat(products).isNotEmpty()
                .hasSize(2)
                .noneMatch(p -> p.getProductCode().equals("P001"));
    }

    @DisplayName("상품 ID로 상세 정보를 조회한다 (Brand 정보 포함).")
    @Test
    @Transactional
    void whenGetProductDetailWithBrand_thenSuccess() {
        // given
        Brand brand = Brand.createBrand("나이키");
        entityManager.persist(brand);

        Product product = Product.createProduct(
                "P001",
                "에어맥스",
                BigDecimal.valueOf(150000),
                50,
                brand
        );
        Product savedProduct = productRepository.registerProduct(product);

        entityManager.flush();
        entityManager.clear();

        // when
        Product result = productService.getProductDetail(savedProduct.getId());

        // then
        assertAll(
                () -> assertThat(result.getId()).isEqualTo(savedProduct.getId()),
                () -> assertThat(result.getProductCode()).isEqualTo("P001"),
                () -> assertThat(result.getProductName()).isEqualTo("에어맥스"),
                () -> assertThat(result.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(150000)),
                () -> assertThat(result.getStock()).isEqualTo(50),
                () -> assertThat(result.getBrand()).isNotNull(),
                () -> assertThat(result.getBrand().getBrandName()).isEqualTo("나이키")
        );
    }

    @DisplayName("상품 ID로 상세 정보를 조회한다 (ProductLike 정보 포함).")
    @Test
    @Transactional
    void whenGetProductDetailWithProductLikes_thenSuccess() {
        // given
        Brand brand = Brand.createBrand("아디다스");
        entityManager.persist(brand);

        Product product = Product.createProduct(
                "P002",
                "슈퍼스타",
                BigDecimal.valueOf(120000),
                100,
                brand
        );
        Product savedProduct = productRepository.registerProduct(product);

        entityManager.flush();
        entityManager.clear();

        // when
        Product result = productService.getProductDetail(savedProduct.getId());

        // then
        assertAll(
                () -> assertThat(result.getId()).isEqualTo(savedProduct.getId()),
                () -> assertThat(result.getProductCode()).isEqualTo("P002"),
                () -> assertThat(result.getProductName()).isEqualTo("슈퍼스타"),
                () -> assertThat(result.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(120000)),
                () -> assertThat(result.getStock()).isEqualTo(100),
                () -> assertThat(result.getBrand()).isNotNull(),
                () -> assertThat(result.getBrand().getBrandName()).isEqualTo("아디다스"),
                () -> assertThat(result.getProductLikes()).isNotNull(),
                () -> assertThat(result.getProductLikes()).isEmpty()
        );
    }

    @DisplayName("존재하지 않는 상품 ID로 조회 시 NOT_FOUND 예외가 발생한다.")
    @Test
    void whenGetProductDetailWithInvalidId_thenNotFound() {
        // given
        Long invalidProductId = 99999L;

        // when // then
        CoreException exception = assertThrows(CoreException.class, () -> {
            productService.getProductDetail(invalidProductId);
        });

        assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(exception.getCustomMessage()).isEqualTo("상품을 찾을 수 없습니다");
    }

    @DisplayName("삭제된 상품 조회 시 NOT_FOUND 예외가 발생한다.")
    @Test
    @Transactional
    void whenGetProductDetailWithDeletedProduct_thenNotFound() {
        // given
        Brand brand = Brand.createBrand("테스트브랜드");
        entityManager.persist(brand);

        Product product = Product.createProduct(
                "P003",
                "삭제될 상품",
                BigDecimal.valueOf(20000),
                30,
                brand
        );
        Product savedProduct = productRepository.registerProduct(product);

        // 상품 삭제
        savedProduct.delete();

        entityManager.flush();
        entityManager.clear();

        // when // then
        CoreException exception = assertThrows(CoreException.class, () -> {
            productService.getProductDetail(savedProduct.getId());
        });

        assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        assertThat(exception.getCustomMessage()).isEqualTo("상품을 찾을 수 없습니다");
    }
}
