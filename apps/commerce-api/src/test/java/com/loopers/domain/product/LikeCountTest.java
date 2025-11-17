package com.loopers.domain.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LikeCountTest {

    @DisplayName("좋아요 수를 생성할 때,")
    @Nested
    class CreateLikeCount {

        @DisplayName("좋아요 수가 올바르게 주어지면 정상적으로 생성된다.")
        @Test
        void createsLikeCount_whenCountIsValid() {
            // arrange
            Integer count = 100;

            // act
            LikeCount likeCount = LikeCount.createLikeCount(count);

            // assert
            assertThat(likeCount.getCount()).isEqualTo(count);
        }

        @DisplayName("좋아요 수가 0이면 정상적으로 생성된다.")
        @Test
        void createsLikeCount_whenCountIsZero() {
            // arrange
            Integer count = 0;

            // act
            LikeCount likeCount = LikeCount.createLikeCount(count);

            // assert
            assertThat(likeCount.getCount()).isEqualTo(0);
        }

        @DisplayName("좋아요 수가 null이면 BAD_REQUEST가 발생한다.")
        @Test
        void throwsException_whenCountIsNull() {
            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                LikeCount.createLikeCount(null);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("좋아요 수는 필수입니다.");
        }

        @DisplayName("좋아요 수가 음수이면 BAD_REQUEST가 발생한다.")
        @ParameterizedTest
        @ValueSource(ints = {-1, -100})
        void throwsException_whenCountIsNegative(int invalidCount) {
            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                LikeCount.createLikeCount(invalidCount);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("좋아요 수는 0 이상이어야 합니다.");
        }
    }
}

