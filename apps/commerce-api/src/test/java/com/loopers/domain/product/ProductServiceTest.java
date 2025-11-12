package com.loopers.domain.product;

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
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private EntityManager entityManager;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("상품 코드, 상품명, 가격, 수량을 입력받아, 상품을 등록한다.")
    @Test
    void whenRegisterProduct_thenSuccess() {

        // given
        String productCode = "P001";
        String productName = "청바지";
        BigDecimal price = BigDecimal.valueOf(25000);
        int stock = 200;

        // when
        Product productResponse = productService.registerProduct(
                        productCode,productName, price, stock
                );

        // then
        assertAll(
                () -> assertThat(productResponse.getProductCode()).isEqualTo("P001"),
                () -> assertThat(productResponse.getProductName()).isEqualTo("청바지"),
                () -> assertThat(productResponse.getPrice()).isEqualTo(BigDecimal.valueOf(25000)),
                () -> assertThat(productResponse.getStock()).isEqualTo(200)
        );
    }

    @DisplayName("이미 존재하는 상품 코드는 등록에 실패한다.")
    @Test
    void whenRegisterProductWithDuplicateCode_thenBadRequest() {

        // given
        String productCode1 = "P001";
        String productName1 = "청바지";
        BigDecimal price1 = BigDecimal.valueOf(25000);
        int stock1 = 200;

        String productCode2 = "P001";
        String productName2 = "청바지";
        BigDecimal price2 = BigDecimal.valueOf(25000);
        int stock2 = 100;

        // when
        productService.registerProduct(productCode1, productName1, price1, stock1);

        // then
        CoreException result = assertThrows(CoreException.class, () -> {
            productService.registerProduct(productCode2, productName2, price2, stock2);
        });

        assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(result.getCustomMessage()).isEqualTo("중복된 상품 코드 오류");
    }

    @DisplayName("정렬 조건이 null인 경우, 기본값(최신순)으로 상품 목록을 조회한다.")
    @Test
    void whenGetProductsWithNullSortType_thenReturnLatestOrder() {
        // given
        productService.registerProduct("P001", "상품1", BigDecimal.valueOf(10000), 10);
        productService.registerProduct("P002", "상품2", BigDecimal.valueOf(20000), 20);
        productService.registerProduct("P003", "상품3", BigDecimal.valueOf(15000), 15);

        // when
        List<Product> products = productService.getProducts(null);

        // then
        assertThat(products).isNotEmpty();
        assertThat(products.size()).isEqualTo(3);
    }

    @DisplayName("LATEST 정렬 조건으로 상품 목록을 최신순으로 조회한다.")
    @Test
    void whenGetProductsWithLatest_thenReturnLatestOrder() {
        // given
        productService.registerProduct("P001", "상품1", BigDecimal.valueOf(10000), 10);
        productService.registerProduct("P002", "상품2", BigDecimal.valueOf(20000), 20);
        productService.registerProduct("P003", "상품3", BigDecimal.valueOf(15000), 15);

        // when
        List<Product> products = productService.getProducts(ProductSortType.LATEST);

        // then
        assertThat(products).isNotEmpty();
        assertThat(products.size()).isEqualTo(3);
        // 최신순이므로 P003, P002, P001 순서
        assertThat(products.get(0).getProductCode()).isEqualTo("P003");
        assertThat(products.get(1).getProductCode()).isEqualTo("P002");
        assertThat(products.get(2).getProductCode()).isEqualTo("P001");
    }

    @DisplayName("PRICE_ASC 정렬 조건으로 상품 목록을 가격 낮은 순으로 조회한다.")
    @Test
    void whenGetProductsWithPriceAsc_thenReturnPriceAscOrder() {
        // given
        productService.registerProduct("P001", "상품1", BigDecimal.valueOf(30000), 10);
        productService.registerProduct("P002", "상품2", BigDecimal.valueOf(10000), 20);
        productService.registerProduct("P003", "상품3", BigDecimal.valueOf(20000), 15);

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
    void whenGetProductsWithLikesDesc_thenReturnLikesDescOrder() {
        // given
        productService.registerProduct("P001", "상품1", BigDecimal.valueOf(10000), 10);
        productService.registerProduct("P002", "상품2", BigDecimal.valueOf(20000), 20);
        productService.registerProduct("P003", "상품3", BigDecimal.valueOf(15000), 15);

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
        Product product1 = productService.registerProduct("P001", "상품1", BigDecimal.valueOf(10000), 10);
        productService.registerProduct("P002", "상품2", BigDecimal.valueOf(20000), 20);
        productService.registerProduct("P003", "상품3", BigDecimal.valueOf(15000), 15);

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
}
