package com.loopers.domain.like;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import com.loopers.infrastructure.brand.BrandJpaRepository;
import com.loopers.interfaces.api.like.ProductLikeDto;
import com.loopers.support.error.CoreException;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

        @DisplayName("이미 좋아요한 상품에 다시 좋아요를 등록하면, 중복 등록되지 않는다.")
        @Test
        void likeAcceptanceTest2() {
            // arrange
            ProductLikeDto.LikeResponse response = productLikeService.likeProduct(user.getUserId(), product.getId());

            // act
            ProductLikeDto.LikeResponse response2 = productLikeService.likeProduct(user.getUserId(), product.getId());

            // assert
            assertAll(
                    () -> assertThat(response2.totalLikes()).isEqualTo(1L),
                    () -> {
                        Product updatedProduct = productRepository.findById(product.getId()).get();
                        assertThat(updatedProduct.getTotalLikes()).isEqualTo(1L);
                    }
            );
        }

        @DisplayName("존재하지 않는 사용자로 좋아요를 등록하면, 예외가 발생한다.")
        @Test
        void likeAcceptanceTest3() {
            // act & assert
            assertThatThrownBy(() ->
                    productLikeService.likeProduct("user", product.getId())
            )
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("해당 사용자를 찾을 수 없습니다");
        }

        @DisplayName("존재하지 않는 상품에 좋아요를 등록하면, 예외가 발생한다.")
        @Test
        void likeAcceptanceTest4() {
            // act & assert
            assertThatThrownBy(() ->
                    productLikeService.likeProduct(user.getUserId(), 999999L)
            )
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("해당 상품을 찾을 수 없습니다");
        }
    }
}
