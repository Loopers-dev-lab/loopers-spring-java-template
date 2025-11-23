package com.loopers.domain.like;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

class LikeTest {
    @DisplayName("좋아요 등록할 때, ")
    @Nested
    class Register {

        @DisplayName("유저 ID가 음수 혹은 null일 경우, 등록 실패한다.")
        @ParameterizedTest
        @NullSource
        @ValueSource(longs = {-1L, -100L})
        void fails_whenUserIdIsNullOrNegative(Long invalidUserId) {
            // arrange + act + assert
            assertThatThrownBy(() -> Like.create(invalidUserId, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("유저 ID는 음수 혹은 null일 수 없습니다.");
        }

        @DisplayName("상품 ID가 음수 혹은 null일 경우, 등록 실패한다.")
        @ParameterizedTest
        @NullSource
        @ValueSource(longs = {-1L, -100L})
        void fails_whenProductIdIsNullOrNegative(Long invalidProductId) {
            // arrange + act + assert
            assertThatThrownBy(() -> Like.create(1L, invalidProductId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("상품 ID는 음수 혹은 null일 수 없습니다.");
        }

        @DisplayName("정상적으로 등록되는 경우 deletedAt이 null이다.")
        @Test
        void succeeds_whenLikeIsRegistered() {
            // arrange
            Long userId = 1L;
            Long productId = 1L;
            Like like = Like.create(userId, productId);

            // act
            like.like();

            // assert
            assertAll(
                    () -> assertThat(like).isNotNull(),
                    () -> assertThat(like.getUserId()).isEqualTo(userId),
                    () -> assertThat(like.getProductId()).isEqualTo(productId),
                    () -> assertThat(like.getDeletedAt()).isNull()
            );
        }

        @DisplayName("이미 좋아요한 상태에서 다시 좋아요해도 deletedAt 값이 변하지 않는다.")
        @Test
        void succeeds_whenRegisterCalledMultipleTimes() {
            // arrange
            Long userId = 1L;
            Long productId = 1L;
            Like like = Like.create(userId, productId);

            // act
            like.like();
            LocalDateTime firstStatus = like.getDeletedAt();

            like.like();

            // assert
            assertThat(like.getDeletedAt()).isEqualTo(firstStatus);
        }
    }

    @DisplayName("좋아요 취소할 때, ")
    @Nested
    class Cancel {
        @DisplayName("정상적으로 취소되는 경우 deletedAt이 null이 아니다.")
        @Test
        void succeeds_whenLikeIsCanceled() {
            // arrange
            Like like = Like.create(1L, 1L);

            // act
            like.unlike();

            // assert
            assertThat(like.getDeletedAt()).isNotNull();
        }

        @DisplayName("이미 취소한 상태에서 다시 취소해도 deletedAt 값이 변하지 않는다. (멱등성 보장)")
        @Test
        void succeeds_whenCancelCalledMultipleTimes() {
            // arrange
            Like like = Like.create(1L, 1L);

            // act
            like.unlike();
            LocalDateTime firstStatus = like.getDeletedAt();

            like.unlike();

            // assert
            assertThat(like.getDeletedAt()).isEqualTo(firstStatus);
        }
    }
}
