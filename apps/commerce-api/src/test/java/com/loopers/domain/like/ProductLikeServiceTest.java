package com.loopers.domain.like;

import com.loopers.domain.product.ProductLikeInfo;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductLikeServiceTest {

    @Mock ProductLikeRepository productLikeRepository;
    @Mock ProductRepository productRepository;
    @Mock UserRepository userRepository;

    @InjectMocks ProductLikeDomainService service;

    static final String USER_HEADER = "user123";
    static final long USER_ID = 1L;
    static final long PRODUCT_ID = 1L;

    private User stubUser() {
        User u = mock(User.class);
        when(u.getId()).thenReturn(USER_ID);
        return u;
    }

    @Nested
    @DisplayName("좋아요 등록")
    class Like {

        @Test
        @DisplayName("좋아요 등록")
        void productLikeService1() {
            User user = mock(User.class);
            when(user.getId()).thenReturn(USER_ID);

            Product product = mock(Product.class);
            when(product.getId()).thenReturn(PRODUCT_ID);
            when(product.getTotalLikes()).thenReturn(1L);

            ProductLike existingLike = mock(ProductLike.class);

            when(productLikeRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID))
                    .thenReturn(Optional.empty());

            ProductLikeInfo info = service.likeProduct(user, product);

            assertThat(info.liked()).isTrue();
            assertThat(info.totalLikes()).isEqualTo(1L);

            verify(productLikeRepository).save(any(ProductLike.class));
            verify(product).increaseLikes();
            verify(productRepository).save(product);

        }
    }

    @Nested
    @DisplayName("좋아요 취소")
    class UnLike {

        @Test
        @DisplayName("좋아요 취소")
        void productLikeService2() {
            User user = mock(User.class);
            when(user.getId()).thenReturn(USER_ID);

            Product product = mock(Product.class);
            when(product.getId()).thenReturn(PRODUCT_ID);
            when(product.getTotalLikes()).thenReturn(0L);

            ProductLike existingLike = mock(ProductLike.class);

            when(productLikeRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID))
                    .thenReturn(Optional.of(existingLike));

            ProductLikeInfo info = service.unlikeProduct(user, product);

            assertThat(info.liked()).isFalse();
            assertThat(info.totalLikes()).isEqualTo(0L);

            verify(productLikeRepository).delete(existingLike);
            verify(product).decreaseLikes();
        }
    }

    @Nested
    @DisplayName("중복 방지")
    class Idempotency {

        @Test
        @DisplayName("중복 요청시에도 좋아요 수는 총 1")
        void productLikeService3() {
            User user = mock(User.class);
            when(user.getId()).thenReturn(USER_ID);

            Product product = mock(Product.class);
            when(product.getId()).thenReturn(PRODUCT_ID);
            when(product.getTotalLikes()).thenReturn(1L);

            ProductLike existingLike = mock(ProductLike.class);

            when(productLikeRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID))
                    .thenReturn(Optional.of(existingLike));

            ProductLikeInfo info = service.likeProduct(user, product);

            assertThat(info.liked()).isTrue();
            assertThat(info.totalLikes()).isEqualTo(1L);

            verify(productLikeRepository, never()).save(any());
            verify(product, never()).increaseLikes();
            verify(productRepository, never()).save(any());
        }
    }
}
