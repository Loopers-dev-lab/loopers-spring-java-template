package com.loopers.domain.like;

import com.loopers.domain.product.Product;
import com.loopers.domain.user.User;
import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LikeModelTest {
  Like like;
  User mockUser = mock(User.class);
  Product mockProduct = mock(Product.class);

  @DisplayName("좋아요 모델을 생성할 때, ")
  @Nested
  class Create_Like {
    @DisplayName("사용자id와 상품id가 모두 주어지면, 정상적으로 생성된다.")
    @Test
    void 성공_Like_객체생성() {
      //given
      mockUser = mock(User.class);
      when(mockUser.getId()).thenReturn(1L);
      mockProduct = mock(Product.class);
      when(mockProduct.getId()).thenReturn(10L);
      //act
      like = Like.create(mockUser, mockProduct);
      //assert
      assertThat(like).isNotNull();
      assertThat(like.getUser().getId()).isEqualTo(mockUser.getId());
      assertThat(like.getProduct().getId()).isEqualTo(mockProduct.getId());
    }
  }

  @Nested
  class Valid_Like {
    @BeforeEach
    void setup() {
      mockUser = mock(User.class);
      when(mockUser.getId()).thenReturn(1L);
      mockProduct = mock(Product.class);
      when(mockProduct.getId()).thenReturn(10L);
    }

    @Test
    void 실패_사용자ID_오류() {
      //given
      when(mockUser.getId()).thenReturn(null);
      assertThatThrownBy(() -> {
        like = Like.create(mockUser, mockProduct);
      }).isInstanceOf(CoreException.class).hasMessageContaining("사용자ID는 비어있을 수 없습니다.");
    }

    @Test
    void 실패_상품ID_오류() {
      //given
      when(mockProduct.getId()).thenReturn(null);
      assertThatThrownBy(() -> {
        like = Like.create(mockUser, mockProduct);
      }).isInstanceOf(CoreException.class).hasMessageContaining("상품ID는 비어있을 수 없습니다.");
    }
  }
}
