package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductTest {

    @DisplayName("상품의 이름은 필수값이다.")
    @Test
    void whenRegisterProductInvalidName_throwBadRequest() {

        CoreException result = assertThrows(CoreException.class, () -> {
            Product.builder()
                    .price(BigDecimal.valueOf(10000))
                    .stock(100)
                    .build();
        });

        assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(result.getCustomMessage()).isEqualTo("상품 이름 오류");

    }

    @DisplayName("상품의 가격은 0이상의 정수여야 한다.")
    @Test
    void whenRegisterProductInvalidPrice_throwBadRequest() {

        CoreException result = assertThrows(CoreException.class, () -> {
            Product.builder()
                    .productName("상품1")
                    .price(BigDecimal.valueOf(-1))
                    .stock(100)
                    .build();
        });

        assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(result.getCustomMessage()).isEqualTo("상품 가격 오류");

    }

}
