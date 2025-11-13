package com.loopers.domain.like;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import com.loopers.interfaces.api.like.ProductLikeDto;
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

    @InjectMocks ProductLikeService service;

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
            User user = stubUser();
            Product product = mock(Product.class);

            when(userRepository.find(USER_HEADER)).thenReturn(Optional.of(user));
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
            when(productLikeRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID))
                    .thenReturn(Optional.empty());

            when(productRepository.save(product)).thenReturn(product);
            when(product.getTotalLikes()).thenReturn(1L);

            when(productLikeRepository.save(any(ProductLike.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            ProductLikeDto.LikeResponse resp = service.likeProduct(USER_HEADER, PRODUCT_ID);

            assertThat(resp.liked()).isTrue();
            assertThat(resp.totalLikes()).isEqualTo(1L);

            verify(product).increaseLikes();
            verify(productRepository).save(product);
            verify(productLikeRepository).save(any(ProductLike.class));
        }
    }

    @Nested
    @DisplayName("좋아요 취소")
    class UnLike {

        @Test
        @DisplayName("좋아요 취소")
        void productLikeService2() {
            User user = stubUser();
            Product product = mock(Product.class);
            ProductLike existing = mock(ProductLike.class);

            when(userRepository.find(USER_HEADER)).thenReturn(Optional.of(user));
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
            when(productLikeRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID))
                    .thenReturn(Optional.of(existing));

            when(productRepository.save(product)).thenReturn(product);
            when(product.getTotalLikes()).thenReturn(0L);

            ProductLikeDto.LikeResponse resp = service.unlikeProduct(USER_HEADER, PRODUCT_ID);

            assertThat(resp.liked()).isFalse();
            assertThat(resp.totalLikes()).isEqualTo(0L);

            verify(productLikeRepository).delete(existing);
            verify(product).decreaseLikes();
            verify(productRepository).save(product);
        }
    }

    @Nested
    @DisplayName("중복 방지")
    class Idempotency {

        @Test
        @DisplayName("중복 요청시에도 좋아요 수는 총 1")
        void productLikeService3() {
            User user = stubUser();
            Product product = mock(Product.class);
            ProductLike existing = mock(ProductLike.class);

            when(userRepository.find(USER_HEADER)).thenReturn(Optional.of(user));
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));

            when(productLikeRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID))
                    .thenReturn(Optional.empty(), Optional.of(existing));

            when(productRepository.save(product)).thenReturn(product);
            when(productLikeRepository.save(any(ProductLike.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            when(product.getTotalLikes()).thenReturn(1L);

            ProductLikeDto.LikeResponse first = service.likeProduct(USER_HEADER, PRODUCT_ID);
            ProductLikeDto.LikeResponse second = service.likeProduct(USER_HEADER, PRODUCT_ID);

            assertThat(first.liked()).isTrue();
            assertThat(first.totalLikes()).isEqualTo(1L);
            assertThat(second.liked()).isTrue();
            assertThat(second.totalLikes()).isEqualTo(1L);

            verify(product, times(1)).increaseLikes();
            verify(productRepository, times(1)).save(product);
            verify(productLikeRepository, times(1)).save(any(ProductLike.class));
            verify(productLikeRepository, never()).delete(any());
        }
    }
}
