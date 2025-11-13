package com.loopers.domain.like;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.interfaces.api.like.ProductLikeDto;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
class ProductLikeServiceIntegrationTest {

    @Autowired
    private ProductLikeService productLikeService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductLikeRepository productLikeRepository;

    @Autowired
    private BrandJpaRepository brandJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private User user;
    private Product product;

    @BeforeEach
    void setUp() {

        user = userRepository.save(
                User.create("user123", "user@test.com", "2000-01-01", Gender.MALE)
        );

        Brand brand = brandJpaRepository.save(Brand.create("브랜드A"));
        product = productRepository.save(
                Product.create("상품A", "설명", 10_000, 100L, brand.getId())
        );
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("좋아요 등록")
    @Nested
    class LikeProduct {

        @DisplayName("사용자가 상품에 좋아요를 등록하면, 좋아요가 생성되고 상품의 totalLikes가 증가한다.")
        @Test
        void likeAcceptanceTest1() {
            // act
            ProductLikeDto.LikeResponse response = productLikeService.likeProduct(user.getUserId(), product.getId());

            // assert
            assertAll(
                    () -> assertThat(response.totalLikes()).isEqualTo(1L),
                    () -> {
                        Optional<ProductLike> like = productLikeRepository
                                .findByUserIdAndProductId(user.getId(), product.getId());
                        assertThat(like).isPresent();
                    },
                    () -> {
                        Product updatedProduct = productRepository.findById(product.getId()).get();
                        assertThat(updatedProduct.getTotalLikes()).isEqualTo(1L);
                    }
            );
        }
    }
}
