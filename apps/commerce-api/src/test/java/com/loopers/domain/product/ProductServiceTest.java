package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

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
}
