package com.loopers.domain.like;

import com.loopers.application.like.LikeFacade;
import com.loopers.domain.common.vo.Money;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductLikeCount;
import com.loopers.domain.product.ProductLikeCountRepository;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static com.loopers.support.fixture.UserFixtures.createValidUser;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
class LikeServiceIntegrationTest {
    @Autowired
    private LikeService likeService;

    @Autowired
    private LikeFacade likeFacade;

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductLikeCountRepository productLikeCountRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    private Product createValidProduct() {
        return Product.create(1L, "상품이름1", Money.of(1000));
    }

    @DisplayName("유저가 좋아요 등록 시,")
    @Nested
    class LikeRegister {
        @DisplayName("중복 등록이 아닌 경우, Like가 저장되고 상품의 likeCount가 1 증가한다.")
        @Test
        void succeeds_whenFirstLike() {

            // arrange
            User user = userRepository.save(createValidUser());
            Product product = productRepository.save(
                    createValidProduct()
            );

            // act
            likeFacade.addLike(user.getId(), product.getId());

            // assert
            Optional<Like> result = likeService.findLike(user.getId(), product.getId());
            Optional<ProductLikeCount> productLikeCountResult = productLikeCountRepository.findById(product.getId());

            assertAll(
                    () -> assertThat(result).isPresent(),
                    () -> assertThat(productLikeCountResult).isPresent(),
                    () -> assertThat(productLikeCountResult.get().getLikeCount()).isEqualTo(1)
            );
        }

        @DisplayName("중복 등록인 경우, Like와 상품의 likeCount는 변하지 않는다. (멱등성 유지)")
        @Test
        void doesNothing_whenDuplicateLike() {

            // arrange
            User user = userRepository.save(createValidUser());
            Product product = productRepository.save(
                    createValidProduct()
            );
            likeFacade.addLike(user.getId(), product.getId());

            // act
            likeFacade.addLike(user.getId(), product.getId());

            // assert
            Optional<Like> result = likeService.findLike(user.getId(), product.getId());
            Optional<ProductLikeCount> productLikeCountResult = productLikeCountRepository.findById(product.getId());
            assertAll(
                    () -> assertThat(result).isPresent(),
                    () -> assertThat(productLikeCountResult).isPresent(),
                    () -> assertThat(productLikeCountResult.get().getLikeCount()).isEqualTo(1)
            );
        }

    }

    @DisplayName("유저의 좋아요 취소 시,")
    @Nested
    class UnLike {
        @DisplayName("중복 취소가 아닌 경우, Like가 hard-deleted되고 상품의 likeCount가 1 감소한다.")
        @Test
        void succeeds_whenFirstCancel() {

            // arrange
            User user = userRepository.save(createValidUser());
            Product product = productRepository.save(
                    createValidProduct()
            );
            likeFacade.addLike(user.getId(), product.getId());

            // act
            likeFacade.removeLike(user.getId(), product.getId());

            // assert

            Optional<Like> result = likeService.findLike(user.getId(), product.getId());
            Optional<ProductLikeCount> productLikeCountResult = productLikeCountRepository.findById(product.getId());

            assertAll(
                    () -> assertThat(result).isEmpty(),
                    () -> assertThat(productLikeCountResult).isPresent(),
                    () -> assertThat(productLikeCountResult.get().getLikeCount()).isEqualTo(0)
            );
        }

        @DisplayName("중복 취소인 경우, Like와 상품의 likeCount는 변하지 않는다. (멱등성 유지)")
        @Test
        void doesNothing_whenDuplicateCancel() {

            // arrange
            User user = userRepository.save(createValidUser());
            Product product = productRepository.save(
                    createValidProduct()
            );

            likeFacade.addLike(user.getId(), product.getId());
            likeFacade.removeLike(user.getId(), product.getId());

            // act
            likeFacade.removeLike(user.getId(), product.getId());

            // assert
            Optional<Like> result = likeService.findLike(user.getId(), product.getId());
            Optional<ProductLikeCount> productLikeCountResult = productLikeCountRepository.findById(product.getId());

            assertAll(
                    () -> assertThat(result).isEmpty(),
                    () -> assertThat(productLikeCountResult).isPresent(),
                    () -> assertThat(productLikeCountResult.get().getLikeCount()).isEqualTo(0)
            );
        }
    }
}
