package com.loopers.domain.like;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.*;

/**
 * packageName : com.loopers.domain.like
 * fileName     : LikeServiceIntegrationTest
 * author      : byeonsungmun
 * date        : 2025. 11. 14.
 * description :
 * ===========================================
 * DATE         AUTHOR       NOTE
 * -------------------------------------------
 * 2025. 11. 14.     byeonsungmun       최초 생성
 */
@SpringBootTest
class LikeServiceIntegrationTest {

    private static final long LIKE_COUNT_AWAIT_TIMEOUT_MILLIS = 2_000L;
    private static final long LIKE_COUNT_AWAIT_INTERVAL_MILLIS = 50L;

    @Autowired
    private LikeService likeService;

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DatabaseCleanUp cleanUp;

    @AfterEach
    void tearDown() {
        cleanUp.truncateAllTables();
    }

    @Nested
    @DisplayName("좋아요 기능 통합 테스트")
    class LikeTests {

        @Test
        @DisplayName("좋아요 생성 성공 → 좋아요 저장 + 상품의 likeCount 증가")
        void likeSuccess() {
            // given
            User user = userRepository.save(new User("user1", "u1@mail.com", "1990-01-01", "MALE"));
            Product product = productRepository.save(Product.create(1L, "상품A", 1000L, 10L));

            // when
            likeService.like(user.getUserId(), product.getId());

            // then
            Like saved = likeRepository.findByUserIdAndProductId("user1", 1L).orElse(null);
            assertThat(saved).isNotNull();

            awaitProductLikeCount(1L, 1L);
        }

        @Test
        @DisplayName("중복 좋아요 시 likeCount 증가 안 하고 저장도 안 됨")
        void duplicateLike() {
            // given
            userRepository.save(new User("user1", "u1@mail.com", "1990-01-01", "MALE"));
            productRepository.save(Product.create(1L, "상품A", 1000L, 10L));

            likeService.like("user1", 1L);
            awaitProductLikeCount(1L, 1L);

            // when
            likeService.like("user1", 1L); // 중복 호출

            // then
            long likeCount = likeRepository.countByProductId(1L);
            assertThat(likeCount).isEqualTo(1L);

            awaitProductLikeCount(1L, 1L); // 증가 X
        }

        @Test
        @DisplayName("좋아요 취소 성공 → like 삭제 + 상품의 likeCount 감소")
        void unlikeSuccess() {
            // given
            userRepository.save(new User("user1", "u1@mail.com", "1990-01-01", "MALE"));
            productRepository.save(Product.create(1L, "상품A", 1000L, 10L));

            likeService.like("user1", 1L);
            awaitProductLikeCount(1L, 1L);

            // when
            likeService.unlike("user1", 1L);

            // then
            Like like = likeRepository.findByUserIdAndProductId("user1", 1L).orElse(null);
            assertThat(like).isNull();

            awaitProductLikeCount(1L, 0L);
        }

        @Test
        @DisplayName("없는 좋아요 취소 시 likeCount 감소 안 함")
        void unlikeNonExisting() {
            // given
            userRepository.save(new User("user1", "u1@mail.com", "1990-01-01", "MALE"));
            Product product = Product.create(1L, "상품A", 1000L, 10L);
            product.increaseLikeCount();
            product.increaseLikeCount();
            product.increaseLikeCount();
            product.increaseLikeCount();
            product.increaseLikeCount();

            productRepository.save(product);
            // when — 호출은 해도
            likeService.unlike("user1", 1L);

            // then — 변화 없음
            Product updated = productRepository.findById(1L).get();
            assertThat(updated.getLikeCount()).isEqualTo(5L);
        }

        @Test
        @DisplayName("countByProductId 정상 조회")
        void countTest() {
            // given
            userRepository.save(new User("user1", "u1@mail.com", "1990-01-01", "MALE"));
            userRepository.save(new User("user2", "u2@mail.com", "1991-01-01", "MALE"));
            productRepository.save(Product.create(1L, "상품A", 1000L, 10L));

            likeService.like("user1", 1L);
            likeService.like("user2", 1L);

            // when
            long count = likeService.countByProductId(1L);

            // then
            assertThat(count).isEqualTo(2L);
        }
    }

    private void awaitProductLikeCount(Long productId, long expectedCount) {
        long waited = 0L;
        while (waited <= LIKE_COUNT_AWAIT_TIMEOUT_MILLIS) {
            long current = productRepository.findById(productId)
                    .map(Product::getLikeCount)
                    .orElseThrow();
            if (current == expectedCount) {
                return;
            }
            sleep(LIKE_COUNT_AWAIT_INTERVAL_MILLIS);
            waited += LIKE_COUNT_AWAIT_INTERVAL_MILLIS;
        }
        fail(String.format("productId=%d likeCount가 %d 에 도달하지 않았습니다.", productId, expectedCount));
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(interruptedException);
        }
    }
}
