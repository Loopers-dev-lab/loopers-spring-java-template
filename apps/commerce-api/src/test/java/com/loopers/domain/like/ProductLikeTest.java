package com.loopers.domain.like;

import com.loopers.domain.product.Product;
import com.loopers.domain.user.User;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductLikeTest {

    @DisplayName("좋아요 등록 시 유저 정보는 필수이다.")
    @Test
    void whenCreateLikeInvalidUser_throwException() {
        // given
        Product mockProduct = mock(Product.class);
        when(mockProduct.getProductCode()).thenReturn("P001");
        when(mockProduct.getProductName()).thenReturn("상품1");

        // when // then
        CoreException result = assertThrows(CoreException.class, () -> {
            ProductLike like = ProductLike.addLike(null, mockProduct);
        });

        assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(result.getCustomMessage()).isEqualTo("좋아요를 누를 사용자가 필요합니다");

    }

    @DisplayName("좋아요 등록 시 상품 정보는 필수이다.")
    @Test
    void whenCreateLikeInvalidProduct_throwException() {
        // given
        User mockUser = mock(User.class);
        when(mockUser.getUserId()).thenReturn("test123");
        when(mockUser.getEmail()).thenReturn("test123@test.com");

        // when // then
        CoreException result = assertThrows(CoreException.class, () -> {
            ProductLike like = ProductLike.addLike(mockUser, null);
        });

        assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        assertThat(result.getCustomMessage()).isEqualTo("좋아요를 누를 상품이 필요합니다");

    }

    @DisplayName("유저와 상품이 유효하면 좋아요를 등록할 수 있다.")
    @Test
    void whenCreateLikeWithValidUserAndProduct_success() {
        // given
        User mockUser = mock(User.class);
        when(mockUser.getUserId()).thenReturn("test123");
        when(mockUser.getEmail()).thenReturn("test123@test.com");

        Product mockProduct = mock(Product.class);
        when(mockProduct.getProductCode()).thenReturn("P001");
        when(mockProduct.getProductName()).thenReturn("상품1");

        // when
        ProductLike like = ProductLike.addLike(mockUser, mockProduct);

        // then
        assertThat(like).isNotNull();
        assertThat(like.getLikeUser()).isEqualTo(mockUser);
        assertThat(like.getLikeProduct()).isEqualTo(mockProduct);
        assertThat(like.isSameUserAndProduct(mockUser, mockProduct)).isTrue();
    }

    @DisplayName("같은 유저가 같은 상품에 중복으로 좋아요를 등록하려고 하면 이미 존재함을 확인할 수 있다.")
    @Test
    void whenCheckDuplicateLike_returnTrue() {
        // given
        User mockUser = mock(User.class);
        Product mockProduct = mock(Product.class);

        ProductLike like = ProductLike.addLike(mockUser, mockProduct);

        // when
        boolean isDuplicate = like.isSameUserAndProduct(mockUser, mockProduct);

        // then
        assertThat(isDuplicate).isTrue();
    }

}
