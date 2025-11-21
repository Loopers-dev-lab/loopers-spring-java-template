package com.loopers.domain.like;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LikeModelTest {

    @Mock
    LikeRepository likeRepository;

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

        // when
        likeService.likeProduct(USER_ID, PRODUCT_ID);

        // then
        verify(likeRepository).save(argThat(like ->
                like.getUserId().equals(USER_ID) &&
                        like.getProductId().equals(PRODUCT_ID)
        ));

    }

    @Test
    @DisplayName("중복 등록은 무시된다(멱등)")
    void like_ignores_when_present() {
        // given
        when(likeRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID))
                .thenReturn(Optional.of(new Like(USER_ID, PRODUCT_ID)));

        // when
        likeService.likeProduct(USER_ID, PRODUCT_ID);

        // then
        verify(likeRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 등록된 좋아요는 취소된다")
    void cancel_deletes_when_present() {
        // given
        Like like = new Like(USER_ID, PRODUCT_ID);
        when(likeRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID))
                .thenReturn(Optional.of(like));

        // when
        likeService.cancleLikeProduct(USER_ID, PRODUCT_ID);

        // then
        verify(likeRepository, times(1)).delete(eq(like));
    }

    @Test
    @DisplayName("상품별 좋아요 수를 카운트로 조회할 수 있다")
    void count_by_product() {
        // given
        when(likeRepository.countByProductId(1L)).thenReturn(10000L);
        when(likeRepository.countByProductId(2L)).thenReturn(20000L);

        // when
        Long c1 = likeService.getLikeCount(1L);
        Long c2 = likeService.getLikeCount(2L);

        // then
        assertThat(c1).isEqualTo(10000L);
        assertThat(c2).isEqualTo(20000L);
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

