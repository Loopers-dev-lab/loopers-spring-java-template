package com.loopers.domain.like;

import com.loopers.domain.product.ProductModel;
import com.loopers.domain.product.Brand;
import com.loopers.domain.common.Money;
import com.loopers.domain.common.Quantity;
import com.loopers.domain.user.UserModel;
import com.loopers.domain.user.UserId;
import com.loopers.domain.user.Email;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.BirthDate;
import com.loopers.infrastructure.like.LikeJpaRepository;
import com.loopers.infrastructure.product.ProductJpaRepository;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
class LikeServiceIntegrationTest {
    @Autowired
    private LikeService likeService;

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

    @DisplayName("좋아요 등록/취소")
    @Nested
    class LikeManagement {
        
        @DisplayName("좋아요 등록이 정상 처리된다")
        @Test
        void createsLike_whenAddLikeIsCalled() {
            // arrange
            UserModel user = userJpaRepository.save(
                new UserModel(new UserId("user123"), new Email("user123@user.com"), new Gender("male"), new BirthDate("1999-01-01"))
            );
            ProductModel product = productJpaRepository.save(
                new ProductModel("product123", new Brand("Apple"), new Money(10000), new Quantity(100))
            );

            // act
            likeService.addLike(user, product);

            // assert
            boolean isLiked = likeService.isLiked(user, product);
            assertThat(isLiked).isTrue();
        }

        @DisplayName("좋아요 취소가 정상 처리된다")
        @Test
        void removesLike_whenRemoveLikeIsCalled() {
            // arrange
            UserModel user = userJpaRepository.save(
                new UserModel(new UserId("user123"), new Email("user123@user.com"), new Gender("male"), new BirthDate("1999-01-01"))
            );
            ProductModel product = productJpaRepository.save(
                new ProductModel("product123", new Brand("Apple"), new Money(10000), new Quantity(100))
            );
            likeService.addLike(user, product);

            // act
            likeService.removeLike(user, product);

            // assert
            boolean isLiked = likeService.isLiked(user, product);
            assertThat(isLiked).isFalse();
        }

        @DisplayName("중복 좋아요 방지: 이미 좋아요가 있으면 아무것도 하지 않는다 (멱등성)")
        @Test
        void preventsDuplicateLikes_whenAddLikeIsCalledTwice() {
            // arrange
            UserModel user = userJpaRepository.save(
                new UserModel(new UserId("user123"), new Email("user123@user.com"), new Gender("male"), new BirthDate("1999-01-01"))
            );
            ProductModel product = productJpaRepository.save(
                new ProductModel("product123", new Brand("Apple"), new Money(10000), new Quantity(100))
            );
            likeService.addLike(user, product);
            long likeCountBefore = likeJpaRepository.count();

            // act
            likeService.addLike(user, product); // 중복 호출

            // assert
            long likeCountAfter = likeJpaRepository.count();
            assertThat(likeCountAfter).isEqualTo(likeCountBefore); // 좋아요 수가 증가하지 않음
            assertThat(likeService.isLiked(user, product)).isTrue();
        }

        @DisplayName("중복 취소 방지: 이미 좋아요가 없으면 아무것도 하지 않는다 (멱등성)")
        @Test
        void preventsDuplicateRemoval_whenRemoveLikeIsCalledTwice() {
            // arrange
            UserModel user = userJpaRepository.save(
                new UserModel(new UserId("user123"), new Email("user123@user.com"), new Gender("male"), new BirthDate("1999-01-01"))
            );
            ProductModel product = productJpaRepository.save(
                new ProductModel("product123", new Brand("Apple"), new Money(10000), new Quantity(100))
            );
            likeService.removeLike(user, product); // 처음부터 좋아요 없음
            long likeCountBefore = likeJpaRepository.count();

            // act
            likeService.removeLike(user, product); // 중복 호출

            // assert
            long likeCountAfter = likeJpaRepository.count();
            assertThat(likeCountAfter).isEqualTo(likeCountBefore); // 좋아요 수가 변하지 않음
            assertThat(likeService.isLiked(user, product)).isFalse();
        }

        @DisplayName("좋아요 토글이 정상 동작한다")
        @Test
        void togglesLike_whenToggleLikeIsCalled() {
            // arrange
            UserModel user = userJpaRepository.save(
                new UserModel(new UserId("user123"), new Email("user123@user.com"), new Gender("male"), new BirthDate("1999-01-01"))
            );
            ProductModel product = productJpaRepository.save(
                new ProductModel("product123", new Brand("Apple"), new Money(10000), new Quantity(100))
            );

            // act & assert - 첫 번째 호출: 좋아요 등록
            likeService.toggleLike(user, product);
            assertThat(likeService.isLiked(user, product)).isTrue();

            // act & assert - 두 번째 호출: 좋아요 취소
            likeService.toggleLike(user, product);
            assertThat(likeService.isLiked(user, product)).isFalse();
        }
    }

    @DisplayName("좋아요 수 조회")
    @Nested
    class LikeCount {
        
        @DisplayName("상품의 좋아요 수를 정확히 조회한다")
        @Test
        void returnsCorrectLikeCount_whenMultipleUsersLikeProduct() {
            // arrange
            UserModel user1 = userJpaRepository.save(
                new UserModel(new UserId("user1"), new Email("user1@user.com"), new Gender("male"), new BirthDate("1999-01-01"))
            );
            UserModel user2 = userJpaRepository.save(
                new UserModel(new UserId("user2"), new Email("user2@user.com"), new Gender("female"), new BirthDate("2000-01-01"))
            );
            ProductModel product = productJpaRepository.save(
                new ProductModel("product123", new Brand("Apple"), new Money(10000), new Quantity(100))
            );
            likeService.addLike(user1, product);
            likeService.addLike(user2, product);

            // act
            long likeCount = likeService.getLikeCount(product);

            // assert
            assertThat(likeCount).isEqualTo(2L);
        }
    }
}
