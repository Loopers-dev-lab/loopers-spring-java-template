package com.loopers.domain.like;

import com.loopers.domain.like.service.LikeReadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class LikeReadServiceTest {

    private InMemoryLikeRepository likeRepository;
    private LikeReadService likeReadService;

    @BeforeEach
    void setUp() {
        likeRepository = new InMemoryLikeRepository();
        likeReadService = new LikeReadService(likeRepository);
    }

    @Test
    void should_count_likes_by_product_id() {
        // given
        Long productId = 1L;
        likeRepository.save(new Like("member1", productId));
        likeRepository.save(new Like("member2", productId));
        likeRepository.save(new Like("member3", 2L)); // 다른 상품

        // when
        long count = likeReadService.countByProductId(productId);

        // then
        assertThat(count).isEqualTo(2);
    }

    @Test
    void should_return_zero_when_no_likes_for_product() {
        // given
        Long productId = 1L;

        // when
        long count = likeReadService.countByProductId(productId);

        // then
        assertThat(count).isEqualTo(0);
    }

    @Test
    void should_check_if_member_liked_product() {
        // given
        String memberId = "member1";
        Long productId = 1L;
        likeRepository.save(new Like(memberId, productId));

        // when
        boolean isLiked = likeReadService.isLikedBy(memberId, productId);

        // then
        assertThat(isLiked).isTrue();
    }

    @Test
    void should_return_false_when_member_did_not_like_product() {
        // given
        String memberId = "member1";
        Long productId = 1L;

        // when
        boolean isLiked = likeReadService.isLikedBy(memberId, productId);

        // then
        assertThat(isLiked).isFalse();
    }

    @Test
    void should_return_false_when_member_id_is_null() {
        // given
        String memberId = null;
        Long productId = 1L;
        likeRepository.save(new Like("member1", productId));

        // when
        boolean isLiked = likeReadService.isLikedBy(memberId, productId);

        // then
        assertThat(isLiked).isFalse();
    }

    @Test
    void should_not_check_other_members_likes() {
        // given
        String memberId1 = "member1";
        String memberId2 = "member2";
        Long productId = 1L;
        
        likeRepository.save(new Like(memberId1, productId));

        // when
        boolean member1Liked = likeReadService.isLikedBy(memberId1, productId);
        boolean member2Liked = likeReadService.isLikedBy(memberId2, productId);

        // then
        assertThat(member1Liked).isTrue();
        assertThat(member2Liked).isFalse();
    }
}