package com.loopers.application.like;

import com.loopers.domain.brand.BrandModel;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.like.product.ProductLikeRepository;
import com.loopers.domain.product.ProductModel;
import com.loopers.domain.product.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ProductLikeSyncTest {

    @Autowired
    private ProductLikeFacade productLikeFacade;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private ProductLikeRepository productLikeRepository;

    @Test
    @DisplayName("좋아요 등록/취소 시 count 동기화 테스트")
    void 좋아요_count_동기화_테스트() {
        // arrange
        BrandModel brand = createTestBrand();
        ProductModel product = createTestProduct(brand.getId());
        Long userId = 1L;

        BigDecimal initialLikeCount = product.getLikeCount().getValue();

        // act & assert
        productLikeFacade.addProductLike(userId, product.getId());
        ProductModel updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updatedProduct.getLikeCount().getValue().compareTo(
                initialLikeCount.add(BigDecimal.ONE)
        )).isEqualTo(0);

        // act & ssert
        productLikeFacade.removeProductLike(userId, product.getId());
        ProductModel finalProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(finalProduct.getLikeCount().getValue().compareTo(initialLikeCount))
                .isEqualTo(0);
    }

    @Test
    @DisplayName("토글 기능을 통한 좋아요 동기화 테스트")
    void 토글_좋아요_동기화_테스트() {
        // arrange
        BrandModel brand = createTestBrand();
        ProductModel product = createTestProduct(brand.getId());
        Long userId = 2L;

        BigDecimal initialLikeCount = product.getLikeCount().getValue();

        // act & assert
        productLikeFacade.toggleLike(userId, product.getId());
        ProductModel updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updatedProduct.getLikeCount().getValue().compareTo(
                initialLikeCount.add(BigDecimal.ONE)
        )).isEqualTo(0);
        assertThat(productLikeFacade.isProductLiked(userId, product.getId())).isTrue();

        // act & assert
        productLikeFacade.toggleLike(userId, product.getId());
        ProductModel finalProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(finalProduct.getLikeCount().getValue().compareTo(initialLikeCount))
                .isEqualTo(0);
        assertThat(productLikeFacade.isProductLiked(userId, product.getId())).isFalse();
    }

    @Test
    @DisplayName("여러 사용자의 좋아요 동기화 테스트")
    void 여러_사용자_좋아요_동기화_테스트() {
        // arrange
        BrandModel brand = createTestBrand();
        ProductModel product = createTestProduct(brand.getId());
        Long[] userIds = {10L, 11L, 12L, 13L, 14L};

        BigDecimal initialLikeCount = product.getLikeCount().getValue();

        // act
        for (Long userId : userIds) {
            productLikeFacade.addProductLike(userId, product.getId());
        }

        // assert
        ProductModel updatedProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updatedProduct.getLikeCount().getValue().compareTo(
                initialLikeCount.add(BigDecimal.valueOf(userIds.length))
        )).isEqualTo(0);

        // act
        productLikeFacade.removeProductLike(userIds[0], product.getId());
        productLikeFacade.removeProductLike(userIds[1], product.getId());

        // assert
        ProductModel finalProduct = productRepository.findById(product.getId()).orElseThrow();
        assertThat(finalProduct.getLikeCount().getValue().compareTo(
                initialLikeCount.add(BigDecimal.valueOf(userIds.length - 2))
        )).isEqualTo(0);
    }

    private BrandModel createTestBrand() {
        BrandModel brand = BrandModel.of(
                "동기화테스트브랜드",
                "https://test.com",
                BigDecimal.ZERO,
                true
        );
        return brandRepository.save(brand);
    }

    private ProductModel createTestProduct(Long brandId) {
        ProductModel product = ProductModel.register(
                "동기화테스트상품",
                brandId,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(50000),
                "좋아요 동기화 테스트용 상품",
                "https://test.com/product.jpg",
                "ACTIVE",
                BigDecimal.ZERO
        );
        return productRepository.save(product);
    }
}
