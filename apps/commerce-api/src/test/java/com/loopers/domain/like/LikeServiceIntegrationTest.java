package com.loopers.domain.like;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
class LikeServiceIntegrationTest {

    @Autowired
    private LikeService likeService;

    @MockitoSpyBean
    private LikeRepository likeRepository;

    @DisplayName("좋아요를 등록할 때,")
    @Nested
    class RecordLike {

        @DisplayName("좋아요가 존재하지 않으면 등록된다.")
        @Test
        void recordsLike_whenLikeDoesNotExist() {
            // arrange
            Long userId = 1L;
            Long productId = 1L;

            doReturn(false).when(likeRepository).existsByUserIdAndProductId(userId, productId);

            // act
            likeService.recordLike(userId, productId);

            // verify
            verify(likeRepository, times(1)).existsByUserIdAndProductId(userId, productId);
            verify(likeRepository, times(1)).saveLike(argThat(like ->
                    like.getUserId().equals(userId) && like.getProductId().equals(productId)
            ));

        }

        @DisplayName("이미 좋아요가 등록되어 있으면 멱등하게 동작하여 등록하지 않는다.")
        @Test
        void doesNotRecordLike_whenLikeAlreadyExists() {
            // arrange
            Long userId = 1L;
            Long productId = 1L;

            doReturn(true).when(likeRepository).existsByUserIdAndProductId(userId, productId);

            // act
            likeService.recordLike(userId, productId);

            // verify
            verify(likeRepository, times(1)).existsByUserIdAndProductId(userId, productId);
            verify(likeRepository, never()).saveLike(any(Like.class));
        }
    }

    @DisplayName("좋아요를 취소할 때,")
    @Nested
    class CancelLike {

        @DisplayName("좋아요가 존재하면 취소된다.")
        @Test
        void cancelsLike_whenLikeExists() {
            // arrange
            Long userId = 1L;
            Long productId = 1L;

            doReturn(true).when(likeRepository).existsByUserIdAndProductId(userId, productId);

            // act
            likeService.cancelLike(userId, productId);

            // verify
            verify(likeRepository, times(1)).existsByUserIdAndProductId(userId, productId);
            verify(likeRepository, times(1)).delete(userId, productId);
        }

        @DisplayName("이미 좋아요가 취소되어 있으면 멱등하게 동작하여 취소하지 않는다.")
        @Test
        void doesNotCancelLike_whenLikeAlreadyCancelled() {
            // arrange
            Long userId = 1L;
            Long productId = 1L;

            doReturn(false).when(likeRepository).existsByUserIdAndProductId(userId, productId);

            // act
            likeService.cancelLike(userId, productId);

            // verify
            verify(likeRepository, times(1)).existsByUserIdAndProductId(userId, productId);
            verify(likeRepository, never()).delete(anyLong(), anyLong());
        }
    }

    @DisplayName("좋아요한 상품 ID 목록을 조회할 때,")
    @Nested
    class FindLikedProductIds {

        @DisplayName("좋아요한 상품이 있으면 상품 ID 목록이 반환된다.")
        @Test
        void returnsProductIds_whenLikedProductsExist() {
            // arrange
            Long userId = 1L;
            List<Long> productIds = List.of(1L, 2L, 3L);

            doReturn(productIds).when(likeRepository).findProductIdsByUserId(userId);

            // act
            List<Long> result = likeService.findLikedProductIds(userId);

            // assert
            assertThat(result).hasSize(3);
            assertThat(result).containsExactlyInAnyOrder(1L, 2L, 3L);

            // verify
            verify(likeRepository, times(1)).findProductIdsByUserId(userId);
        }

        @DisplayName("좋아요한 상품이 없으면 빈 목록이 반환된다.")
        @Test
        void returnsEmptyList_whenNoLikedProducts() {
            // arrange
            Long userId = 1L;

            doReturn(List.of()).when(likeRepository).findProductIdsByUserId(userId);

            // act
            List<Long> result = likeService.findLikedProductIds(userId);

            // assert
            assertThat(result).isEmpty();

            // verify
            verify(likeRepository, times(1)).findProductIdsByUserId(userId);
        }
    }
}

