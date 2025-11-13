package com.loopers.domain.product;

import com.loopers.domain.brand.Brand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ProductDetailServiceTest {

    private final ProductDetailService productDetailService = new ProductDetailService();

    @DisplayName("상품 상세 조회 시 Product + Brand + 좋아요 수 정보 조합은 도메인 서비스에서 처리했다")
    @Test
    void combineProductAndBrand() {
        // arrange
        Brand brand = Brand.of(ProductTestFixture.ValidBrand.NAME);
        Product product = Product.of(
            ProductTestFixture.ValidProduct.NAME,
            ProductTestFixture.ValidProduct.PRICE,
            ProductTestFixture.ValidProduct.STOCK,
            brand.getId()
        );
        Long likesCount = 10L;

        // act
        ProductDetail productDetail = productDetailService.combineProductAndBrand(product, brand, likesCount);

        // assert
        assertThat(productDetail.getId()).isEqualTo(product.getId());
        assertThat(productDetail.getName()).isEqualTo(product.getName());
        assertThat(productDetail.getPrice()).isEqualTo(product.getPrice());
        assertThat(productDetail.getStock()).isEqualTo(product.getStock());
        assertThat(productDetail.getBrandId()).isEqualTo(brand.getId());
        assertThat(productDetail.getBrandName()).isEqualTo(brand.getName());
        assertThat(productDetail.getLikesCount()).isEqualTo(likesCount);
    }

    @DisplayName("상품이 null이면 예외가 발생한다.")
    @Test
    void throwsException_whenProductIsNull() {
        // arrange
        Brand brand = Brand.of(ProductTestFixture.ValidBrand.NAME);
        Long likesCount = 0L;

        // act
        IllegalArgumentException result = assertThrows(IllegalArgumentException.class, () -> {
            productDetailService.combineProductAndBrand(null, brand, likesCount);
        });

        // assert
        assertThat(result.getMessage()).contains("상품은 null일 수 없습니다");
    }

    @DisplayName("브랜드가 null이면 예외가 발생한다.")
    @Test
    void throwsException_whenBrandIsNull() {
        // arrange
        Brand brand = Brand.of(ProductTestFixture.ValidBrand.NAME);
        Product product = Product.of(
            ProductTestFixture.ValidProduct.NAME,
            ProductTestFixture.ValidProduct.PRICE,
            ProductTestFixture.ValidProduct.STOCK,
            brand.getId()
        );
        Long likesCount = 0L;

        // act
        IllegalArgumentException result = assertThrows(IllegalArgumentException.class, () -> {
            productDetailService.combineProductAndBrand(product, null, likesCount);
        });

        // assert
        assertThat(result.getMessage()).contains("브랜드 정보는 필수입니다");
    }

    @DisplayName("좋아요 수가 null이면 예외가 발생한다.")
    @Test
    void throwsException_whenLikesCountIsNull() {
        // arrange
        Brand brand = Brand.of(ProductTestFixture.ValidBrand.NAME);
        Product product = Product.of(
            ProductTestFixture.ValidProduct.NAME,
            ProductTestFixture.ValidProduct.PRICE,
            ProductTestFixture.ValidProduct.STOCK,
            brand.getId()
        );

        // act
        IllegalArgumentException result = assertThrows(IllegalArgumentException.class, () -> {
            productDetailService.combineProductAndBrand(product, brand, null);
        });

        // assert
        assertThat(result.getMessage()).contains("좋아요 수는 null일 수 없습니다");
    }

}

