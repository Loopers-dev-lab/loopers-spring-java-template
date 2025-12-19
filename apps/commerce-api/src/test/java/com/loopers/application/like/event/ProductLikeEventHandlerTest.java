package com.loopers.application.like.event;

import com.loopers.domain.like.event.ProductLikedEvent;
import com.loopers.domain.product.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductLikeEventHandlerTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductLikeEventHandler productLikeEventHandler;

    @Test
    @DisplayName("좋아요 이벤트를 받으면 상품의 좋아요 수를 증가시킨다")
    void handleProductLikedTest1() {
        // arrange
        Long productId = 1L;
        Long userId = 100L;
        ProductLikedEvent event = ProductLikedEvent.liked(productId, userId);

        // act
        productLikeEventHandler.handleProductLiked(event);

        // assert
        verify(productRepository, times(1)).incrementLikeCount(productId);
        verify(productRepository, never()).decrementLikeCount(productId);
    }

    @Test
    @DisplayName("좋아요 취소 이벤트를 받으면 상품의 좋아요 수를 감소시킨다")
    void handleProductLikedTest2() {
        // arrange
        Long productId = 1L;
        Long userId = 100L;
        ProductLikedEvent event = ProductLikedEvent.unliked(productId, userId);

        // act
        productLikeEventHandler.handleProductLiked(event);

        // assert
        verify(productRepository, times(1)).decrementLikeCount(productId);
        verify(productRepository, never()).incrementLikeCount(productId);
    }

    @Test
    @DisplayName("좋아요 집계 실패해도 이벤트 핸들러는 예외를 던지지 않는다")
    void handleProductLikedTest3() {
        // arrange
        Long productId = 999L;
        Long userId = 100L;
        ProductLikedEvent event = ProductLikedEvent.liked(productId, userId);
        doThrow(new RuntimeException("DB error"))
                .when(productRepository).incrementLikeCount(productId);

        // act - 예외 없이 정상 종료
        productLikeEventHandler.handleProductLiked(event);

        // assert
        verify(productRepository, times(1)).incrementLikeCount(productId);
    }

    @Test
    @DisplayName("좋아요 취소 집계 실패해도 이벤트 핸들러는 예외를 던지지 않는다")
    void handleProductLikedTest4() {
        // arrange
        Long productId = 999L;
        Long userId = 100L;
        ProductLikedEvent event = ProductLikedEvent.unliked(productId, userId);
        doThrow(new RuntimeException("DB error"))
                .when(productRepository).decrementLikeCount(productId);

        // act - 예외 없이 정상 종료
        productLikeEventHandler.handleProductLiked(event);

        // assert
        verify(productRepository, times(1)).decrementLikeCount(productId);
    }
}
