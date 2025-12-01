package com.loopers.domain.like;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

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
        @Transactional
        void likeSuccess() {
            // given
            User user = userRepository.save(new User("user1", "u1@mail.com", "1990-01-01", "MALE"));
            Product product = productRepository.save(Product.create(1L, "상품A", 1000L, 10L));

            // when
            likeService.like(user.getUserId(), product.getId());

            // then
            Like saved = likeRepository.findByUserIdAndProductId("user1", 1L).orElse(null);
            assertThat(saved).isNotNull();

            Product updated = productRepository.findById(1L).get();
            assertThat(updated.getLikeCount()).isEqualTo(1L);
        }

        @Test
        @DisplayName("중복 좋아요 시 likeCount 증가 안 하고 저장도 안 됨")
        @Transactional
        void duplicateLike() {
            // given
            userRepository.save(new User("user1", "u1@mail.com", "1990-01-01", "MALE"));
            productRepository.save(Product.create(1L, "상품A", 1000L, 10L));

            likeService.like("user1", 1L);

            // when
            likeService.like("user1", 1L); // 중복 호출

            // then
            long likeCount = likeRepository.countByProductId(1L);
            assertThat(likeCount).isEqualTo(1L);

            Product updated = productRepository.findById(1L).get();
            assertThat(updated.getLikeCount()).isEqualTo(1L); // 증가 X
        }

        @Test
        @DisplayName("좋아요 취소 성공 → like 삭제 + 상품의 likeCount 감소")
        @Transactional
        void unlikeSuccess() {
            // given
            userRepository.save(new User("user1", "u1@mail.com", "1990-01-01", "MALE"));
            productRepository.save(Product.create(1L, "상품A", 1000L, 10L));

            likeService.like("user1", 1L);

            // when
            likeService.unlike("user1", 1L);

            // then
            Like like = likeRepository.findByUserIdAndProductId("user1", 1L).orElse(null);
            assertThat(like).isNull();

            Product updated = productRepository.findById(1L).get();
            assertThat(updated.getLikeCount()).isEqualTo(0L);
        }

        @Test
        @DisplayName("없는 좋아요 취소 시 likeCount 감소 안 함")
        @Transactional
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
        @Transactional
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
}
