package com.loopers.domain.like;

import com.loopers.domain.common.vo.Money;
import com.loopers.domain.like.Like;
import com.loopers.domain.like.InMemoryLikeRepository;
import com.loopers.domain.like.service.LikeService;
import com.loopers.domain.product.InMemoryProductRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.vo.Stock;
import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * LikeService 단위 테스트
 * - DB 저장/삭제 로직만 테스트 (Redis 캐시는 이벤트 리스너에서 처리)
 */
class LikeServiceTest {

    private InMemoryLikeRepository likeRepository;
    private InMemoryProductRepository productRepository;
    private LikeService likeService;

    @BeforeEach
    void setUp() {
        likeRepository = new InMemoryLikeRepository();
        productRepository = new InMemoryProductRepository();
        likeService = new LikeService(likeRepository, productRepository);
    }

    @Test
    void should_like_product_successfully() {
        // given
        Product product = createProduct();
        productRepository.saveWithId(1L, product);
        Long memberId = 1L;

        // when
        Product result = likeService.like(memberId, 1L);

        // then
        assertThat(likeRepository.existsByMemberIdAndProductId(memberId, 1L)).isTrue();
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void should_handle_idempotent_like_operation() {
        // given
        Product product = createProduct();
        productRepository.saveWithId(1L, product);
        Long memberId = 1L;

        // when
        likeService.like(memberId, 1L);
        likeService.like(memberId, 1L); // 두 번 호출

        // then
        assertThat(likeRepository.existsByMemberIdAndProductId(memberId, 1L)).isTrue();
        // DB에는 한 번만 저장됨 (중복 방지) - 멱등성 확인
    }

    @Test
    void should_unlike_product_successfully() {
        // given
        Product product = createProduct();
        productRepository.saveWithId(1L, product);
        Long memberId = 1L;

        // 먼저 좋아요 등록
        likeService.like(memberId, 1L);

        // when
        Product result = likeService.unlike(memberId, 1L);

        // then
        assertThat(likeRepository.existsByMemberIdAndProductId(memberId, 1L)).isFalse();
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void should_handle_idempotent_unlike_operation() {
        // given
        Product product = createProduct();
        productRepository.saveWithId(1L, product);
        Long memberId = 1L;

        // when - 좋아요가 없는 상태에서 취소 시도 (두 번)
        likeService.unlike(memberId, 1L);
        likeService.unlike(memberId, 1L);

        // then - 예외 없이 정상 동작 (멱등성 확인)
        assertThat(likeRepository.existsByMemberIdAndProductId(memberId, 1L)).isFalse();
    }

    @Test
    void should_throw_exception_when_like_nonexistent_product() {
        // given
        Long memberId = 1L;
        Long nonExistentProductId = 999L;

        // when & then
        assertThatThrownBy(() -> likeService.like(memberId, nonExistentProductId))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining("상품을 찾을 수 없습니다");
    }

    @Test
    void should_throw_exception_when_unlike_nonexistent_product() {
        // given
        Long memberId = 1L;
        Long nonExistentProductId = 999L;

        // 좋아요가 존재한다고 가정하고 like repository에 저장
        likeRepository.save(new Like(memberId, nonExistentProductId));

        // when & then
        assertThatThrownBy(() -> likeService.unlike(memberId, nonExistentProductId))
                .isInstanceOf(CoreException.class)
                .hasMessageContaining("상품을 찾을 수 없습니다");
    }

    @Test
    void should_handle_multiple_members_liking_same_product() {
        // given
        Product product = createProduct();
        productRepository.saveWithId(1L, product);
        Long member1 = 1L;
        Long member2 = 2L;

        // when
        likeService.like(member1, 1L);
        likeService.like(member2, 1L);

        // then
        assertThat(likeRepository.existsByMemberIdAndProductId(member1, 1L)).isTrue();
        assertThat(likeRepository.existsByMemberIdAndProductId(member2, 1L)).isTrue();
    }

    private Product createProduct() {
        return new Product(
                1L,
                "테스트 상품",
                "테스트 상품 설명",
                Money.of(10000),
                Stock.of(100)
        );
    }
}