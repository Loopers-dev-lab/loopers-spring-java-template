package com.loopers.domain.like;

import com.loopers.domain.product.Money;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.Stock;
import com.loopers.domain.like.event.LikeCreatedEvent;
import com.loopers.domain.like.event.LikeDeletedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LikeModelTest {

    @Mock
    LikeRepository likeRepository;
    @Mock
    ProductRepository productRepository;
    @Mock
    ApplicationEventPublisher eventPublisher;

    @InjectMocks
    LikeService likeService;

    static final String USER_ID = "sky980221";
    static final Long PRODUCT_ID = 19980221L;
    static final Long PRODUCT_ID_2 = 19980303L;

    @Test
    @DisplayName("좋아요가 없는 경우 새 좋아요를 생성한다")
    void like_registers_when_absent() {
        // given
        when(likeRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID)).thenReturn(Optional.empty());
        Product product = Product.builder()
                .brandId(1L)
                .name("P1")
                .price(new Money(BigDecimal.valueOf(1000)))
                .stockQuantity(new Stock(10))
                .likeCount(0L)
                .build();
        when(productRepository.findByIdForUpdate(PRODUCT_ID)).thenReturn(Optional.of(product));

        // when
        likeService.likeProduct(USER_ID, PRODUCT_ID);

        // then
        verify(likeRepository).save(argThat(like ->
                like.getUserId().equals(USER_ID) &&
                        like.getProductId().equals(PRODUCT_ID)
        ));
        verify(eventPublisher, times(1)).publishEvent(any(LikeCreatedEvent.class));

    }

    @Test
    @DisplayName("중복 등록은 무시된다(멱등)")
    void like_ignores_when_present() {
        // given
        when(likeRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID))
                .thenReturn(Optional.of(new Like(USER_ID, PRODUCT_ID)));
        Product product = Product.builder()
                .brandId(1L)
                .name("P1")
                .price(new Money(BigDecimal.valueOf(1000)))
                .stockQuantity(new Stock(10))
                .likeCount(0L)
                .build();
        when(productRepository.findByIdForUpdate(PRODUCT_ID)).thenReturn(Optional.of(product));

        // when
        likeService.likeProduct(USER_ID, PRODUCT_ID);

        // then
        verify(likeRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("이미 등록된 좋아요는 취소된다")
    void cancel_deletes_when_present() {
        // given
        Like like = new Like(USER_ID, PRODUCT_ID);
        when(likeRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID))
                .thenReturn(Optional.of(like));
        Product product = Product.builder()
                .brandId(1L)
                .name("P1")
                .price(new Money(BigDecimal.valueOf(1000)))
                .stockQuantity(new Stock(10))
                .likeCount(1L)
                .build();
        when(productRepository.findByIdForUpdate(PRODUCT_ID)).thenReturn(Optional.of(product));

        // when
        likeService.cancleLikeProduct(USER_ID, PRODUCT_ID);

        // then
        verify(likeRepository, times(1)).delete(eq(like));
        verify(eventPublisher, times(1)).publishEvent(any(LikeDeletedEvent.class));
    }

    @Test
    @DisplayName("좋아요/취소 동작은 Product.likeCount에 반영된다")
    void like_and_unlike_update_product_like_count() {
        // given
        Product product = Product.builder()
                .brandId(1L)
                .name("P1")
                .price(new Money(BigDecimal.valueOf(1000)))
                .stockQuantity(new Stock(10))
                .likeCount(0L)
                .build();
        when(productRepository.findByIdForUpdate(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(likeRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID))
                .thenReturn(Optional.empty()) // for like
                .thenReturn(Optional.of(new Like(USER_ID, PRODUCT_ID))); // for cancel

        // when
        likeService.likeProduct(USER_ID, PRODUCT_ID);
        likeService.cancleLikeProduct(USER_ID, PRODUCT_ID);

        // then
        verify(eventPublisher, times(1)).publishEvent(any(LikeCreatedEvent.class));
        verify(eventPublisher, times(1)).publishEvent(any(LikeDeletedEvent.class));
    }

    @Test
    @DisplayName("유저가 좋아요 한 상품의 목록을 조회할 수 있다")
    void get_liked_products_by_user() {
        // given
        Like like1 = new Like(USER_ID, PRODUCT_ID);
        Like like2 = new Like(USER_ID, PRODUCT_ID_2);
        when(likeRepository.findAllByUserId(USER_ID))
                .thenReturn(List.of(like1, like2));

        // when
        List<Like> likedProducts = likeService.getUserLikeProduct(USER_ID);

        // then
        assertThat(likedProducts).hasSize(2);
        assertThat(likedProducts).extracting("productId")
                .containsExactlyInAnyOrder(PRODUCT_ID, PRODUCT_ID_2);
    }
}

