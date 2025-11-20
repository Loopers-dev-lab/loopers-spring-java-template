package com.loopers.application.like;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.loopers.domain.like.Like;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.vo.Stock;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.infrastructure.like.LikeJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.utils.DatabaseCleanUp;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class LikeFacadeIntegrationTest {
    @Autowired
    private LikeFacade likeFacade;
    @Autowired
    private LikeJpaRepository likeJpaRepository;
    @Autowired
    private ProductJpaRepository productJpaRepository;
    @Autowired
    private UserJpaRepository userJpaRepository;
    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("좋아요를 생성할 때,")
    @Nested
    class Create {
        @Test
        void 유저_PK_ID와_상품_PK_ID가_주어지면_정상적으로_생성된다() {
            // arrange
            Long userId = 1L;
            Product product = productJpaRepository.save(Product.create("testA", 1000, new Stock(2), 1L));

            // act
            LikeInfo likeInfo = likeFacade.createLike(userId, product.getId());

            // assert
            assertAll(
                    () -> assertThat(likeInfo.id()).isNotNull(),
                    () -> assertThat(likeInfo.userId()).isEqualTo(1L),
                    () -> assertThat(likeInfo.productId()).isEqualTo(product.getId())
            );
        }

        @Test
        void 여러_유저가_동시에_좋아요를_요청하면_좋아요_카운트가_정확히_증가한다() {
            // given
            int userCount = 10;
            List<User> users = IntStream.range(0, userCount)
                    .mapToObj(i -> userJpaRepository.save(
                            User.create("user" + i, "user" + i + "@test.com",
                                    LocalDate.of(2020, 1, 1), Gender.MALE)
                    ))
                    .toList();
            Product product = productJpaRepository.save(Product.create("p1", 1000, new Stock(10), 1L));

            // when
            List<CompletableFuture<Void>> futures = users.stream()
                    .map(user -> CompletableFuture.runAsync(() -> {
                        likeFacade.createLike(user.getId(), product.getId());
                    }))
                    .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // then
            Product sut = productJpaRepository.findById(product.getId()).orElseThrow();
            assertThat(sut.getLikeCount().getValue()).isEqualTo(10);
        }
    }

    @DisplayName("좋아요를 취소할 때,")
    @Nested
    class Delete {
        @Test
        void 좋아요에_등록된_유저_PK_ID와_상품_PK_ID가_주어지면_정상적으로_삭제된다() {
            // arrange
            Long userId = 1L;
            Product saveProduct = Product.create("testA", 1000, new Stock(2), 1L);
            saveProduct.likeCountIncrease(2);
            Product product = productJpaRepository.save(saveProduct);
            likeJpaRepository.save(Like.create(userId, product.getId()));

            // act
            likeFacade.unLike(userId, product.getId());

            // assert
            assertThat(likeJpaRepository.findAll()).hasSize(0);
        }

        @Test
        void 여러_유저가_동시에_좋아요취소를_요청하면_좋아요_카운트가_정확히_감소한다() {
            // given
            int userCount = 10;
            List<User> users = IntStream.range(0, userCount)
                    .mapToObj(i -> userJpaRepository.save(
                            User.create("user" + i, "user" + i + "@test.com",
                                    LocalDate.of(2020, 1, 1), Gender.MALE)
                    ))
                    .toList();
            Product product = productJpaRepository.save(Product.create("p1", 1000, new Stock(10), 1L));
            product.likeCountIncrease(10);
            productJpaRepository.save(product);
            users.forEach(user -> likeJpaRepository.save(Like.create(user.getId(), product.getId())));

            // when
            List<CompletableFuture<Void>> futures = users.stream()
                    .map(user -> CompletableFuture.runAsync(() -> {
                        likeFacade.unLike(user.getId(), product.getId());
                    }))
                    .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // then
            Product sut = productJpaRepository.findById(product.getId()).orElseThrow();
            assertThat(sut.getLikeCount().getValue()).isEqualTo(0);
        }
    }
}
