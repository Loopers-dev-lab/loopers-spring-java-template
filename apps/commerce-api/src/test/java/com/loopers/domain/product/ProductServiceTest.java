package com.loopers.domain.product;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest
class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @DisplayName("상품을 등록한다.")
    @Test
    void whenRegisterProduct_thenSuccess() {

        // given
        Product product = Product.builder()
                .productName("청바지")
                .price(BigDecimal.valueOf(25000))
                .stock(200)
                .build();

        // when
        Product productResponse = productService.registerProduct(product);

        // then
        assertAll(
                () -> assertThat(productResponse.getProductName()).isEqualTo("청바지"),
                () -> assertThat(productResponse.getPrice()).isEqualTo(BigDecimal.valueOf(25000)),
                () -> assertThat(productResponse.getStock()).isEqualTo(200)
        );
    }
}
