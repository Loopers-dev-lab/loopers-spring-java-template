package com.loopers.application.like;

import com.loopers.domain.like.LikeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("LikeFacade 단위 테스트")
class LikeFacadeTest {

    @Test
    @DisplayName("addLike는 도메인 서비스에 위임한다 (멱등)")
    void addLike_delegates() {
        // given
        LikeService likeService = mock(LikeService.class);
        LikeFacade facade = new LikeFacade(likeService);

        // when
        facade.addLike("user-1", 10L);

        // then
        verify(likeService, times(1)).addLike("user-1", 10L);
        verifyNoMoreInteractions(likeService);
    }

    @Test
    @DisplayName("removeLike는 도메인 서비스에 위임한다 (멱등)")
    void removeLike_delegates() {
        // given
        LikeService likeService = mock(LikeService.class);
        LikeFacade facade = new LikeFacade(likeService);

        // when
        facade.removeLike("user-1", 10L);

        // then
        verify(likeService, times(1)).removeLike("user-1", 10L);
        verifyNoMoreInteractions(likeService);
    }

    @Test
    @DisplayName("isLiked는 결과를 그대로 반환한다")
    void isLiked_returns() {
        // given
        LikeService likeService = mock(LikeService.class);
        when(likeService.isLiked("u", 1L)).thenReturn(true);
        LikeFacade facade = new LikeFacade(likeService);

        // when
        boolean liked = facade.isLiked("u", 1L);

        // then
        assertThat(liked).isTrue();
        verify(likeService, times(1)).isLiked("u", 1L);
        verifyNoMoreInteractions(likeService);
    }

    @Test
    @DisplayName("getLikeCount는 결과를 그대로 반환한다")
    void getLikeCount_returns() {
        // given
        LikeService likeService = mock(LikeService.class);
        when(likeService.getLikeCount(1L)).thenReturn(7);
        LikeFacade facade = new LikeFacade(likeService);

        // when
        int count = facade.getLikeCount(1L);

        // then
        assertThat(count).isEqualTo(7);
        verify(likeService, times(1)).getLikeCount(1L);
        verifyNoMoreInteractions(likeService);
    }

    @Test
    @DisplayName("getLikedProductIds는 결과를 그대로 반환한다")
    void getLikedProductIds_returns() {
        // given
        LikeService likeService = mock(LikeService.class);
        when(likeService.getLikedProductIds("u")).thenReturn(List.of(1L, 2L, 3L));
        LikeFacade facade = new LikeFacade(likeService);

        // when
        List<Long> ids = facade.getLikedProductIds("u");

        // then
        assertThat(ids).containsExactly(1L, 2L, 3L);
        verify(likeService, times(1)).getLikedProductIds("u");
        verifyNoMoreInteractions(likeService);
    }
}
