package com.loopers.domain.like;

import com.loopers.domain.common.vo.Money;
import com.loopers.domain.product.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(MockitoExtension.class)
public class LikeDomainServiceTest {

    @InjectMocks
    private  LikeDomainService likeDomainService;

    private Product productWithId(Long id) {
        Product product = Product.create(1L, "상품이름1", Money.of(1000));
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    @Nested
    @DisplayName("좋아요 반영 시")
    class ApplyLike {

        @Test
        @DisplayName("중복 등록이 아닌 경우, Like가 저장되고 상품의 likeCount가 1 증가한다.")
        void succeeds_whenFirstLike() {
            // arrange
            Product product = productWithId(1L);
            Like like = Like.create(1L, product.getId());

            // act
            likeDomainService.applyLike(1L, product, like, true);

            // assert
            assertThat(like.isActive()).isTrue();
            assertThat(product.getLikeCount()).isEqualTo(1);
        }

        @DisplayName("중복 등록인 경우, Like와 상품의 likeCount는 변하지 않는다. (멱등성 유지)")
        @Test
        void doesNothing_whenDuplicateLike() {
            // arrange
            Product product = productWithId(1L);
            Like like = Like.create(1L, product.getId());
            int firstLikeCount = product.getLikeCount();

            // act
            likeDomainService.applyLike(1L, product, like, false);

            // assert
            assertThat(like.isActive()).isTrue();
            assertThat(product.getLikeCount()).isEqualTo(firstLikeCount);
        }
    }
    @DisplayName("유저의 좋아요 취소 시,")
    @Nested
    class UnLike {
        @DisplayName("중복 취소가 아닌 경우, Like가 soft-deleted되고 상품의 likeCount가 1 감소한다.")
        @Test
        void succeeds_whenFirstCancel() {
            // arrange
            Product product = productWithId(1L);
            Like like = Like.create(1L, product.getId());
            product.increaseLikeCount();

            // act
            likeDomainService.applyUnLike(1L, product, like);

            // assert
            assertThat(like.isActive()).isFalse();
            assertThat(product.getLikeCount()).isEqualTo(0);
        }

        @DisplayName("중복 취소인 경우, Like와 상품의 likeCount는 변하지 않는다. (멱등성 유지)")
        @Test
        void doesNothing_whenDuplicateCancel() {
            // arrange
            Product product = productWithId(1L);
            Like like = Like.create(1L, product.getId());
            like.unlike();
            int firstLikeCount = product.getLikeCount();

            // act
            likeDomainService.applyUnLike(1L, product, like);

            // assert
            assertThat(like.isActive()).isFalse();
            assertThat(product.getLikeCount()).isEqualTo(firstLikeCount);

        }
    }
}
