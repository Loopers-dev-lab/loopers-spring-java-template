package com.loopers.domain.product.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class LikeCountTest {
    @DisplayName("LikeCount을 생성할 때, ")
    @Nested
    class Create {

        @Test
        void 좋아요수는_값이_없으면_0으로_정상_생성된다() {
            LikeCount likeCount = new LikeCount();
            assertThat(likeCount.getValue()).isEqualTo(0);
        }

        @Test
        void 좋아요수가_음수면_예외가_발생한다() {
            assertThatThrownBy(() -> new LikeCount(-1))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("좋아요 수는 0 이상이어야 합니다.");
        }
    }

    @DisplayName("LikeCount를 증가, 감소할 때, ")
    @Nested
    class IncreaseAndDecrease {

        @Test
        void 좋아요수를_증가시킬수_있다() {
            // arrange
            LikeCount likeCount = new LikeCount(1);

            // act
            LikeCount sut = likeCount.increase(1);

            // assert
            assertThat(sut.getValue()).isEqualTo(2);
        }

        @Test
        void 좋아요수를_증가시_음수면_오류가_발생한다() {
            // arrange
            LikeCount likeCount = new LikeCount(1);

            // act && assert
            assertThatThrownBy(() -> likeCount.increase(-1))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("증가할 좋아요 수는 음수일 수 없습니다.");
        }

        @Test
        void 좋아요수를_감소시킬수_있다() {
            // arrange
            LikeCount likeCount = new LikeCount(1);

            // act
            LikeCount sut = likeCount.decrease(1);

            // assert
            assertThat(sut.getValue()).isEqualTo(0);
        }

        @Test
        void 좋아요수를_감소시_음수면_오류가_발생한다() {
            // arrange
            LikeCount likeCount = new LikeCount(1);

            // act && assert
            assertThatThrownBy(() -> likeCount.decrease(-1))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("감소할 좋아요 수는 음수일 수 없습니다.");
        }

        @Test
        void 좋아요수를_감소시킨_값이_음수면_오류가_발생한다() {
            // arrange
            LikeCount likeCount = new LikeCount(1);

            // act && assert
            assertThatThrownBy(() -> likeCount.decrease(2))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("좋아요 수는 0보다 작을 수 없습니다.");
        }
    }
}
