package com.loopers.application.like;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.loopers.domain.like.LikeRepository;
import com.loopers.domain.money.Money;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.User;
import com.loopers.domain.user.User.Gender;
import com.loopers.domain.user.UserRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class LikeFacadeIntegrationTest {

  @Autowired
  LikeFacade likeFacade;

  @Autowired
  LikeRepository likeRepository;

  @Autowired
  UserRepository userRepository;

  @Autowired
  ProductRepository productRepository;

  @Autowired
  private DatabaseCleanUp databaseCleanUp;

  @AfterEach
  void tearDown() {
    databaseCleanUp.truncateAllTables();
  }

  @DisplayName("좋아요 요청 시")
  @Nested
  class LikeProduct {

    @DisplayName("첫 좋아요 요청 시, like info가 반환되고 DB에 저장된다.")
    @Test
    void like_success_whenFirstLike() {
      // arrange
      User user = userRepository.save(new User("userA", "a@email.com", "2025-11-11", Gender.MALE));
      Product product = productRepository.save(new Product(1L, "상품A", "설명", new Money(10000L), 100));

      // act
      LikeInfo result = likeFacade.like(user.getId(), product.getId());

      // assert
      assertAll(
          () -> assertThat(result.id()).isNotNull(),
          () -> assertThat(result.userId()).isEqualTo(user.getId()),
          () -> assertThat(result.productId()).isEqualTo(product.getId()),

          () -> assertThat(likeRepository.countByProductId(product.getId())).isEqualTo(1)
      );
    }

    @DisplayName("두 번째 사용자가 좋아요 요청 시, 갱신 전 카운트(2)가 반환된다.")
    @Test
    void like_success_whenSecondLike() {
      // arrange
      User user1 = userRepository.save(new User("userA", "a@email.com", "2025-11-11", Gender.MALE));
      User user2 = userRepository.save(new User("userB", "b@email.com", "2025-11-11", Gender.FEMALE));
      Product product = productRepository.save(new Product(1L, "상품A", "설명", new Money(10000L), 100));

      likeFacade.like(user1.getId(), product.getId());

      assertThat(likeRepository.countByProductId(product.getId())).isEqualTo(1);

      // act
      LikeInfo result = likeFacade.like(user2.getId(), product.getId());

      // assert
      assertAll(
          () -> assertThat(result.userId()).isEqualTo(user2.getId()),

          () -> assertThat(result.totalLikes()).isEqualTo(2),
          () -> assertThat(likeRepository.countByProductId(product.getId())).isEqualTo(2)
      );
    }

    @DisplayName("이미 좋아요를 누른 사용자가 다시 요청해도, 멱등성이 보장된다.")
    @Test
    void like_idempotent_whenDuplicateRequest() {
      // arrange
      User user = userRepository.save(new User("userA", "a@email.com", "2025-11-11", Gender.MALE));
      Product product = productRepository.save(new Product(1L, "상품A", "설명", new Money(10000L), 100));

      likeFacade.like(user.getId(), product.getId());
      assertThat(likeRepository.countByProductId(product.getId())).isEqualTo(1);

      // act
      LikeInfo result = likeFacade.like(user.getId(), product.getId());

      // assert
      assertAll(
          () -> assertThat(result.totalLikes()).isEqualTo(1L),

          () -> assertThat(likeRepository.countByProductId(product.getId())).isEqualTo(1)
      );
    }
  }

  @DisplayName("좋아요 취소 요청 시")
  @Nested
  class UnlikeProduct {

    @DisplayName("좋아요를 누른 상품을 취소하면, DB에서 삭제되고 갱신된 카운트(0)가 반환된다.")
    @Test
    void unlike_success_and_returns_updated_count() {
      // arrange
      User user = userRepository.save(new User("userA", "a@email.com", "2025-11-11", Gender.MALE));
      Product product = productRepository.save(new Product(1L, "상품A", "설명", new Money(10000L), 100));

      likeFacade.like(user.getId(), product.getId());
      assertThat(likeRepository.countByProductId(product.getId())).isEqualTo(1);

      // act
      long totalLikes = likeFacade.unLike(user.getId(), product.getId());

      // assert
      assertAll(
          () -> assertThat(totalLikes).isZero(),

          () -> assertThat(likeRepository.countByProductId(product.getId())).isZero()
      );
    }
  }
}
