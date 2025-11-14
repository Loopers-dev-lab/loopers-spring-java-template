package com.loopers.domain.productLike;

import com.loopers.domain.like.ProductLikeRepository;
import com.loopers.domain.like.ProductLikeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
public class ProductLikeServiceMockTest {

    @Mock
    ProductLikeRepository productLikeRepository;

    @InjectMocks
    ProductLikeService productLikeService;

    @Test
    @DisplayName("좋아요 등록시 최초 좋아요라면 1을 반환한다")
    void likeSuccess_whenInsertIgnoreReturnsOne() {
        // given
        Long userPkId = 1L;
        Long productId = 10L;

        given(productLikeRepository.insertIgnore(userPkId, productId))
                .willReturn(1);

        // when
        int result = productLikeService.like(userPkId, productId);

        // then
        assertThat(result).isEqualTo(1);
        then(productLikeRepository).should()
                .insertIgnore(userPkId, productId);
    }

    @Test
    @DisplayName("좋아요 등록시 이미 좋아요가 되어있다면 0을 반환한다")
    void likeSuccess_whenInsertIgnoreReturnsZero() {
        // given
        Long userPkId = 1L;
        Long productId = 10L;

        given(productLikeRepository.insertIgnore(userPkId, productId))
                .willReturn(0);

        // when
        int result = productLikeService.like(userPkId, productId);

        // then
        assertThat(result).isEqualTo(0);
        then(productLikeRepository).should()
                .insertIgnore(userPkId, productId);
    }

    @Test
    @DisplayName("좋아요 취소 시 delete 쿼리를 통해 삭제한다")
    void dislikeSuccess_callsDelete() {
        // given
        Long userPkId = 1L;
        Long productId = 10L;

        // when
        productLikeService.dislike(userPkId, productId);

        // then
        then(productLikeRepository).should()
                .delete(userPkId, productId);
    }
}
