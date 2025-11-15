package com.loopers.domain.product;

import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.domain.common.Quantity;
import com.loopers.domain.common.Money;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertAll;

import org.springframework.data.domain.Pageable;

@SpringBootTest
class ProductServiceIntegrationTest {
    @Autowired
    private ProductService productService;

    @Autowired
    private ProductJpaRepository productJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("상품를 조회할 때,")
    @Nested
    class Get {

        @DisplayName("상품 다건 조회 시 상품이 없으면 NOT_FOUND 예외가 발생한다.")
        @Test
        void productService_whenGetProductsIsNotFound() {
            // arrange
            productJpaRepository.save(new ProductModel("제목", new Brand("Apple"), new Money(10000), new Quantity(10)));

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                productService.getProducts(Pageable.ofSize(10), "latest", "Apple");
            });

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }

        @DisplayName("상품 단건 조회 시 상품이 없으면 NOT_FOUND 예외가 발생한다.")
        @Test
        void productService_whenGetProductIsNotFound() {
            // arrange
            Long id = 1L;
            productJpaRepository.findById(id).orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품이 존재하지 않습니다."));

            // act
            CoreException result = assertThrows(CoreException.class, () -> {
                productService.getProduct(id);
            });

            // assert
            assertAll(
                () -> assertThat(result.getErrorType()).isEqualTo(ErrorType.NOT_FOUND)
            );
        }
    }
}
