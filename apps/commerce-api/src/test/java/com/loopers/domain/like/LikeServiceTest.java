package com.loopers.domain.like;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LikeService 도메인 서비스 테스트")
class LikeServiceTest {

    private LikeService likeService;
    private FakeLikeRepository fakeLikeRepository;

    @BeforeEach
    void setUp() {
        fakeLikeRepository = new FakeLikeRepository();
        likeService = new LikeService(fakeLikeRepository);
    }

    @Nested
    @DisplayName("좋아요 등록")
    class AddLikeTest {

        @Test
        @DisplayName("좋아요를 등록할 수 있다")
        void addLike() {
            // given
            String userId = "user123";
            Long productId = 1L;

            // when
            likeService.addLike(userId, productId);

            // then
            assertThat(likeService.isLiked(userId, productId)).isTrue();
            assertThat(likeService.getLikeCount(productId)).isEqualTo(1);
        }

        @Test
        @DisplayName("중복 좋아요 등록은 무시된다 (멱등성)")
        void addLike_duplicate_ignored() {
            // given
            String userId = "user123";
            Long productId = 1L;

            // when
            likeService.addLike(userId, productId);
            likeService.addLike(userId, productId); // 중복 등록

            // then: 좋아요 수는 1개만
            assertThat(likeService.getLikeCount(productId)).isEqualTo(1);
            assertThat(likeService.isLiked(userId, productId)).isTrue();
        }

        @Test
        @DisplayName("예외: userId가 null이면 예외 발생")
        void addLike_withNullUserId() {
            // when & then
            assertThatThrownBy(() -> likeService.addLike(null, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("사용자 ID는 필수입니다");
        }

        @Test
        @DisplayName("예외: userId가 빈 문자열이면 예외 발생")
        void addLike_withEmptyUserId() {
            // when & then
            assertThatThrownBy(() -> likeService.addLike("", 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("사용자 ID는 필수입니다");

            assertThatThrownBy(() -> likeService.addLike("   ", 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("사용자 ID는 필수입니다");
        }

        @Test
        @DisplayName("예외: productId가 null이면 예외 발생")
        void addLike_withNullProductId() {
            // when & then
            assertThatThrownBy(() -> likeService.addLike("user123", null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("상품 ID는 필수입니다");
        }

        @Test
        @DisplayName("여러 사용자가 같은 상품에 좋아요할 수 있다")
        void addLike_multipleUsers() {
            // given
            Long productId = 1L;

            // when
            likeService.addLike("user1", productId);
            likeService.addLike("user2", productId);
            likeService.addLike("user3", productId);

            // then
            assertThat(likeService.getLikeCount(productId)).isEqualTo(3);
        }

        @Test
        @DisplayName("한 사용자가 여러 상품에 좋아요할 수 있다")
        void addLike_multipleProducts() {
            // given
            String userId = "user123";

            // when
            likeService.addLike(userId, 1L);
            likeService.addLike(userId, 2L);
            likeService.addLike(userId, 3L);

            // then
            assertThat(likeService.isLiked(userId, 1L)).isTrue();
            assertThat(likeService.isLiked(userId, 2L)).isTrue();
            assertThat(likeService.isLiked(userId, 3L)).isTrue();
        }
    }

    @Nested
    @DisplayName("좋아요 취소")
    class RemoveLikeTest {

        @Test
        @DisplayName("좋아요를 취소할 수 있다")
        void removeLike() {
            // given
            String userId = "user123";
            Long productId = 1L;
            likeService.addLike(userId, productId);

            // when
            likeService.removeLike(userId, productId);

            // then
            assertThat(likeService.isLiked(userId, productId)).isFalse();
            assertThat(likeService.getLikeCount(productId)).isEqualTo(0);
        }

        @Test
        @DisplayName("존재하지 않는 좋아요 취소는 무시된다 (멱등성)")
        void removeLike_notExists_ignored() {
            // given: 좋아요 없음
            String userId = "user123";
            Long productId = 1L;

            // when & then: 에러 없이 성공
            likeService.removeLike(userId, productId);
            assertThat(likeService.isLiked(userId, productId)).isFalse();
        }

        @Test
        @DisplayName("중복 취소는 무시된다 (멱등성)")
        void removeLike_duplicate_ignored() {
            // given
            String userId = "user123";
            Long productId = 1L;
            likeService.addLike(userId, productId);

            // when: 두 번 취소
            likeService.removeLike(userId, productId);
            likeService.removeLike(userId, productId);

            // then: 에러 없음
            assertThat(likeService.isLiked(userId, productId)).isFalse();
        }

        @Test
        @DisplayName("좋아요 취소 후 재등록 가능하다")
        void addLike_afterRemove() {
            // given
            String userId = "user123";
            Long productId = 1L;
            likeService.addLike(userId, productId);
            likeService.removeLike(userId, productId);

            // when: 재등록
            likeService.addLike(userId, productId);

            // then
            assertThat(likeService.isLiked(userId, productId)).isTrue();
            assertThat(likeService.getLikeCount(productId)).isEqualTo(1);
        }

        @Test
        @DisplayName("특정 사용자의 좋아요만 취소된다")
        void removeLike_onlyTargetUser() {
            // given
            Long productId = 1L;
            likeService.addLike("user1", productId);
            likeService.addLike("user2", productId);
            likeService.addLike("user3", productId);

            // when: user2만 취소
            likeService.removeLike("user2", productId);

            // then
            assertThat(likeService.isLiked("user1", productId)).isTrue();
            assertThat(likeService.isLiked("user2", productId)).isFalse();
            assertThat(likeService.isLiked("user3", productId)).isTrue();
            assertThat(likeService.getLikeCount(productId)).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("좋아요 조회")
    class LikeQueryTest {

        @Test
        @DisplayName("좋아요 여부를 확인할 수 있다")
        void isLiked() {
            // given
            String userId = "user123";
            Long productId = 1L;
            likeService.addLike(userId, productId);

            // then
            assertThat(likeService.isLiked(userId, productId)).isTrue();
            assertThat(likeService.isLiked(userId, 2L)).isFalse();
            assertThat(likeService.isLiked("user456", productId)).isFalse();
        }

        @Test
        @DisplayName("상품의 좋아요 수를 조회할 수 있다")
        void getLikeCount() {
            // given
            Long productId = 1L;
            likeService.addLike("user1", productId);
            likeService.addLike("user2", productId);
            likeService.addLike("user3", productId);

            // when
            int likeCount = likeService.getLikeCount(productId);

            // then
            assertThat(likeCount).isEqualTo(3);
        }

        @Test
        @DisplayName("좋아요가 없는 상품의 좋아요 수는 0이다")
        void getLikeCount_noLikes() {
            // when
            int likeCount = likeService.getLikeCount(999L);

            // then
            assertThat(likeCount).isEqualTo(0);
        }

        @Test
        @DisplayName("경계값: 매우 많은 좋아요를 집계할 수 있다")
        void getLikeCount_manyLikes() {
            // given
            Long productId = 1L;
            for (int i = 1; i <= 1000; i++) {
                likeService.addLike("user" + i, productId);
            }

            // when
            int likeCount = likeService.getLikeCount(productId);

            // then
            assertThat(likeCount).isEqualTo(1000);
        }
    }

    /**
     * Fake Repository for Testing
     */
    static class FakeLikeRepository implements LikeRepository {
        private final Map<String, Like> storage = new HashMap<>();

        private String generateKey(String userId, Long productId) {
            return userId + ":" + productId;
        }

        @Override
        public Like save(Like like) {
            String key = generateKey(like.getUserId(), like.getProductId());
            storage.put(key, like);
            return like;
        }

        @Override
        public Optional<Like> findByUserIdAndProductId(String userId, Long productId) {
            String key = generateKey(userId, productId);
            return Optional.ofNullable(storage.get(key));
        }

        @Override
        public boolean existsByUserIdAndProductId(String userId, Long productId) {
            String key = generateKey(userId, productId);
            return storage.containsKey(key);
        }

        @Override
        public void deleteByUserIdAndProductId(String userId, Long productId) {
            String key = generateKey(userId, productId);
            storage.remove(key);
        }

        @Override
        public int countByProductId(Long productId) {
            return (int) storage.values().stream()
                    .filter(like -> like.getProductId().equals(productId))
                    .count();
        }

        @Override
        public List<Long> findProductIdsByUserId(String userId) {
            return storage.values().stream()
                    .filter(like -> like.getUserId().equals(userId))
                    .map(Like::getProductId)
                    .toList();
        }
    }
}
