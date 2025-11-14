package com.loopers.application.like;

import com.loopers.domain.money.Money;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductService;
import com.loopers.domain.productlike.ProductLikeService;
import com.loopers.domain.stock.Stock;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("LikeFacade 테스트")
class LikeFacadeTest {

  @InjectMocks
  private LikeFacade sut;

  @Mock
  private ProductLikeService productLikeService;

  @Mock
  private ProductService productService;

  @Nested
  @DisplayName("좋아요 등록")
  class LikeProduct {

    @Test
    @DisplayName("좋아요 등록 성공")
    void likeProduct_success() {
      // given
      Long userId = 1L;
      Long productId = 1L;
      Product product = Product.of(
          "상품1",
          Money.of(10000L),
          "설명",
          Stock.of(100L),
          1L
      );

      given(productService.getById(productId)).willReturn(Optional.of(product));
      given(productLikeService.isLiked(userId, productId)).willReturn(false);

      // when
      sut.likeProduct(userId, productId);

      // then
      then(productLikeService).should(times(1)).createLike(userId, productId);
      then(productService).should(times(1)).getById(productId);
    }

    @Test
    @DisplayName("이미 좋아요한 상품 재등록 시 멱등성 보장 (아무것도 하지 않음)")
    void likeProduct_idempotent() {
      // given
      Long userId = 1L;
      Long productId = 1L;
      Product product = Product.of(
          "상품1",
          Money.of(10000L),
          "설명",
          Stock.of(100L),
          1L
      );

      given(productService.getById(productId)).willReturn(Optional.of(product));
      given(productLikeService.isLiked(userId, productId)).willReturn(true);

      // when
      sut.likeProduct(userId, productId);

      // then
      then(productLikeService).should(never()).createLike(userId, productId);
    }

    @Test
    @DisplayName("존재하지 않는 상품 좋아요 시 CoreException 발생")
    void likeProduct_productNotFound() {
      // given
      Long userId = 1L;
      Long productId = 999L;

      given(productService.getById(productId))
          .willThrow(new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));

      // when & then
      assertThatThrownBy(() -> sut.likeProduct(userId, productId))
          .isInstanceOf(CoreException.class)
          .hasMessageContaining("상품을 찾을 수 없습니다.");
    }
  }

  @Nested
  @DisplayName("좋아요 취소")
  class UnlikeProduct {

    @Test
    @DisplayName("좋아요 취소 성공")
    void unlikeProduct_success() {
      // given
      Long userId = 1L;
      Long productId = 1L;
      Product product = Product.of(
          "상품1",
          Money.of(10000L),
          "설명",
          Stock.of(100L),
          1L
      );

      given(productService.getById(productId)).willReturn(Optional.of(product));
      given(productLikeService.isLiked(userId, productId)).willReturn(true);

      // when
      sut.unlikeProduct(userId, productId);

      // then
      then(productLikeService).should(times(1)).deleteLike(userId, productId);
      then(productService).should(times(1)).getById(productId);
    }

    @Test
    @DisplayName("이미 취소한 좋아요 재취소 시 멱등성 보장 (아무것도 하지 않음)")
    void unlikeProduct_idempotent() {
      // given
      Long userId = 1L;
      Long productId = 1L;
      Product product = Product.of(
          "상품1",
          Money.of(10000L),
          "설명",
          Stock.of(100L),
          1L
      );

      given(productService.getById(productId)).willReturn(Optional.of(product));
      given(productLikeService.isLiked(userId, productId)).willReturn(false);

      // when
      sut.unlikeProduct(userId, productId);

      // then
      then(productLikeService).should(never()).deleteLike(userId, productId);
    }

    @Test
    @DisplayName("존재하지 않는 상품 좋아요 취소 시 CoreException 발생")
    void unlikeProduct_productNotFound() {
      // given
      Long userId = 1L;
      Long productId = 999L;

      given(productService.getById(productId))
          .willThrow(new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));

      // when & then
      assertThatThrownBy(() -> sut.unlikeProduct(userId, productId))
          .isInstanceOf(CoreException.class)
          .hasMessageContaining("상품을 찾을 수 없습니다.");
    }
  }
}
