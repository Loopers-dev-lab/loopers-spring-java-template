package com.loopers.domain.like;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

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

        @DisplayName("정상적으로 등록되는 경우 like에 저장된다.")
        @Test
        void succeeds_whenLikeIsRegistered() {
            // arrange
            Long userId = 1L;
            Long productId = 1L;

            // act
            Like like = Like.create(userId, productId);

            // assert
            assertAll(
                    () -> assertThat(like).isNotNull(),
                    () -> assertThat(like.getUserId()).isEqualTo(userId),
                    () -> assertThat(like.getProductId()).isEqualTo(productId)
            );
        }
    }
}
