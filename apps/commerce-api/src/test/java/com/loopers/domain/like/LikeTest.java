package com.loopers.domain.like;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class LikeTest {

    @DisplayName("좋아요을 생성할 때, ")
    @Nested
    class Create {

        @Test
        void 정상적인_값이_들어가면_생성된다() {
            // arrange
            Long userId = 1L;
            Long productId = 1L;

            // act
            Like like = Like.create(userId, productId);

            // assert
            assertAll(
                    () -> assertThat(like.getId()).isNotNull(),
                    () -> assertThat(like.getUserId()).isEqualTo(1L),
                    () -> assertThat(like.getProductId()).isEqualTo(1L)
            );
        }

        @Test
        void 유저_PK_ID나_상품_PK_ID가_null이면_오류가_발생한다() {
            // act & assert
            assertThatThrownBy(() -> Like.create(null, 1L))
                    .isInstanceOfSatisfying(CoreException.class, e -> {
                        assertThat(e.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
                        assertThat(e.getCustomMessage()).isEqualTo("유저는 비어있을 수 없습니다.");
                    });

            assertThatThrownBy(() -> Like.create(1L, null))
                    .isInstanceOfSatisfying(CoreException.class, e -> {
                        assertThat(e.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
                        assertThat(e.getCustomMessage()).isEqualTo("상품은 비어있을 수 없습니다.");
                    });
        }
    }

}
