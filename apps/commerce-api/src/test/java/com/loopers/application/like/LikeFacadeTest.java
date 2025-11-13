package com.loopers.application.like;

import com.loopers.domain.like.Like;
import com.loopers.domain.like.LikeRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;

@DisplayName("LikeFacade 좋아요 등록/취소/중복 방지 흐름 검증")
class LikeFacadeTest {

    private LikeFacade likeFacade;
    private UserRepository userRepository;
    private ProductRepository productRepository;
    private LikeRepository likeRepository;

    private static final String DEFAULT_USER_ID = "testuser";
    private static final Long DEFAULT_USER_INTERNAL_ID = 1L;
    private static final Long DEFAULT_PRODUCT_ID = 1L;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        productRepository = mock(ProductRepository.class);
        likeRepository = mock(LikeRepository.class);

        likeFacade = new LikeFacade(
            likeRepository,
            userRepository,
            productRepository
        );
    }

    @Test
    @DisplayName("좋아요를 등록할 수 있다")
    void addLike_success() {
        // arrange
        setupMocks(DEFAULT_USER_ID, DEFAULT_USER_INTERNAL_ID, DEFAULT_PRODUCT_ID);
        when(likeRepository.findByUserIdAndProductId(DEFAULT_USER_INTERNAL_ID, DEFAULT_PRODUCT_ID))
            .thenReturn(Optional.empty());

        // act
        likeFacade.addLike(DEFAULT_USER_ID, DEFAULT_PRODUCT_ID);

        // assert
        verify(likeRepository).save(any(Like.class));
    }

    @Test
    @DisplayName("좋아요를 취소할 수 있다")
    void removeLike_success() {
        // arrange
        setupMocks(DEFAULT_USER_ID, DEFAULT_USER_INTERNAL_ID, DEFAULT_PRODUCT_ID);
        Like like = Like.of(DEFAULT_USER_INTERNAL_ID, DEFAULT_PRODUCT_ID);
        when(likeRepository.findByUserIdAndProductId(DEFAULT_USER_INTERNAL_ID, DEFAULT_PRODUCT_ID))
            .thenReturn(Optional.of(like));

        // act
        likeFacade.removeLike(DEFAULT_USER_ID, DEFAULT_PRODUCT_ID);

        // assert
        verify(likeRepository).delete(like);
    }

    @Test
    @DisplayName("좋아요는 중복 등록되지 않는다.")
    void addLike_isIdempotent() {
        // arrange
        setupMocks(DEFAULT_USER_ID, DEFAULT_USER_INTERNAL_ID, DEFAULT_PRODUCT_ID);
        when(likeRepository.findByUserIdAndProductId(DEFAULT_USER_INTERNAL_ID, DEFAULT_PRODUCT_ID))
            .thenReturn(Optional.of(Like.of(DEFAULT_USER_INTERNAL_ID, DEFAULT_PRODUCT_ID)));

        // act
        likeFacade.addLike(DEFAULT_USER_ID, DEFAULT_PRODUCT_ID);

        // assert - save는 한 번만 호출되어야 함 (중복 방지)
        verify(likeRepository, never()).save(any(Like.class));
    }

    @Test
    @DisplayName("좋아요는 중복 취소되지 않는다.")
    void removeLike_isIdempotent() {
        // arrange
        setupMocks(DEFAULT_USER_ID, DEFAULT_USER_INTERNAL_ID, DEFAULT_PRODUCT_ID);
        when(likeRepository.findByUserIdAndProductId(DEFAULT_USER_INTERNAL_ID, DEFAULT_PRODUCT_ID))
            .thenReturn(Optional.empty()); // 좋아요 없음

        // act - 좋아요가 없는 상태에서 취소 시도
        likeFacade.removeLike(DEFAULT_USER_ID, DEFAULT_PRODUCT_ID);

        // assert - 예외가 발생하지 않아야 함 (멱등성 보장)
        verify(likeRepository).findByUserIdAndProductId(DEFAULT_USER_INTERNAL_ID, DEFAULT_PRODUCT_ID);
        verify(likeRepository, never()).delete(any(Like.class));
    }

    @Test
    @DisplayName("사용자를 찾을 수 없으면 예외를 던진다")
    void addLike_userNotFound() {
        // arrange
        String unknownUserId = "unknown";
        when(userRepository.findByUserId(unknownUserId)).thenReturn(null);

        // act & assert
        assertThatThrownBy(() -> likeFacade.addLike(unknownUserId, DEFAULT_PRODUCT_ID))
            .isInstanceOf(CoreException.class)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND);
    }

    @Test
    @DisplayName("상품을 찾을 수 없으면 예외를 던진다")
    void addLike_productNotFound() {
        // arrange
        setupMockUser(DEFAULT_USER_ID, DEFAULT_USER_INTERNAL_ID);
        Long nonExistentProductId = 999L;
        when(productRepository.findById(nonExistentProductId)).thenReturn(Optional.empty());

        // act & assert
        assertThatThrownBy(() -> likeFacade.addLike(DEFAULT_USER_ID, nonExistentProductId))
            .isInstanceOf(CoreException.class)
            .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND);
    }

    // Helper methods for test setup

    private void setupMocks(String userId, Long userInternalId, Long productId) {
        setupMockUser(userId, userInternalId);
        setupMockProduct(productId);
    }

    private void setupMockUser(String userId, Long userInternalId) {
        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(userInternalId);
        when(userRepository.findByUserId(userId)).thenReturn(mockUser);
    }

    private void setupMockProduct(Long productId) {
        Product mockProduct = mock(Product.class);
        when(productRepository.findById(productId)).thenReturn(Optional.of(mockProduct));
    }
}

