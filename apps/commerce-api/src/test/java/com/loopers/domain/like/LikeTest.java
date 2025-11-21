package com.loopers.domain.like;

import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class LikeTest {

    @DisplayName("좋아요 생성")
    @Nested
    class CreateLike {

        @DisplayName("정상적인 좋아요가 성공적으로 생성된다")
        @Test
        void shouldCreateLike_whenValidInput() {
            Like like = new Like("member1", 1L);
            
            assertThat(like.getMemberId()).isEqualTo("member1");
            assertThat(like.getProductId()).isEqualTo(1L);
        }

        @DisplayName("회원 ID가 null이면 예외가 발생한다")
        @Test
        void shouldThrowException_whenMemberIdIsNull() {
            assertThatThrownBy(() -> new Like(null, 1L))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("회원 ID는 필수입니다");
        }

        @DisplayName("회원 ID가 빈값이면 예외가 발생한다")
        @Test
        void shouldThrowException_whenMemberIdIsEmpty() {
            assertThatThrownBy(() -> new Like("", 1L))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("회원 ID는 필수입니다");
            
            assertThatThrownBy(() -> new Like("   ", 1L))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("회원 ID는 필수입니다");
        }

        @DisplayName("상품 ID가 null이면 예외가 발생한다")
        @Test
        void shouldThrowException_whenProductIdIsNull() {
            assertThatThrownBy(() -> new Like("member1", null))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("상품 ID는 필수입니다");
        }
    }

    @DisplayName("동등성 검사")
    @Nested
    class Equality {

        @DisplayName("같은 회원ID와 상품ID를 가진 좋아요는 동일하다")
        @Test
        void shouldBeEqual_whenSameMemberIdAndProductId() {
            Like like1 = new Like("member1", 1L);
            Like like2 = new Like("member1", 1L);
            
            assertThat(like1).isEqualTo(like2);
            assertThat(like1.hashCode()).isEqualTo(like2.hashCode());
        }

        @DisplayName("다른 회원ID 또는 상품ID를 가진 좋아요는 다르다")
        @Test
        void shouldNotBeEqual_whenDifferentMemberIdOrProductId() {
            Like like1 = new Like("member1", 1L);
            Like like2 = new Like("member2", 1L);
            Like like3 = new Like("member1", 2L);
            
            assertThat(like1).isNotEqualTo(like2);
            assertThat(like1).isNotEqualTo(like3);
        }
    }
}