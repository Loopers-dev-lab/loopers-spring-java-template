package com.loopers.domain.like.product;

import com.loopers.domain.product.ProductService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
@DisplayName("상품 좋아요 서비스(LikeProductService) 테스트")
public class LikeProductServiceIntegrationTest {

    @MockitoSpyBean
    private LikeProductRepository spyLikeProductRepository;

    @MockitoSpyBean
    private ProductService spyProductService;

    @Autowired
    private LikeProductService likeProductService;

    @DisplayName("좋아요를 등록할 때, ")
    @Nested
    class LikeProductTest {
        @DisplayName("처음 좋아요를 등록하면 새로운 좋아요가 생성된다. (Happy Path)")
        @Test
        void should_createLikeProduct_when_firstLike() {
            // arrange
            Long userId = 1L;
            Long productId = 100L;
            when(spyLikeProductRepository.findByUserIdAndProductId(userId, productId))
                    .thenReturn(Optional.empty());

            // act
            likeProductService.likeProduct(userId, productId);

            // assert
            verify(spyLikeProductRepository).findByUserIdAndProductId(1L, 100L);
            ArgumentCaptor<LikeProduct> captor = ArgumentCaptor.forClass(LikeProduct.class);
            verify(spyLikeProductRepository, times(1)).save(captor.capture());
            LikeProduct savedLike = captor.getValue();
            assertThat(savedLike.getUserId()).isEqualTo(1L);
            assertThat(savedLike.getProductId()).isEqualTo(100L);
        }

        @DisplayName("이미 삭제된 좋아요를 다시 등록하면 복원된다. (Idempotency)")
        @Test
        void should_restoreLikeProduct_when_alreadyDeleted() {
            // arrange
            Long userId = 1L;
            Long productId = 100L;
            LikeProduct deletedLike = LikeProduct.create(userId, productId);
            deletedLike.delete(); // 삭제 상태로 만들기
            when(spyLikeProductRepository.findByUserIdAndProductId(userId, productId))
                    .thenReturn(Optional.of(deletedLike));

            // act
            likeProductService.likeProduct(userId, productId);

            // assert
            verify(spyLikeProductRepository).findByUserIdAndProductId(1L, 100L);
            verify(spyLikeProductRepository, never()).save(any());
            // restore가 호출되었는지 확인 (deletedAt이 null이 되어야 함)
            assertThat(deletedLike.getDeletedAt()).isNull();
        }

        @DisplayName("같은 사용자가 같은 상품에 여러 번 좋아요를 등록해도 한 번만 저장된다. (Idempotency)")
        @Test
        void should_notCreateDuplicate_when_likeMultipleTimes() {
            // arrange
            Long userId = 1L;
            Long productId = 100L;
            LikeProduct existingLike = LikeProduct.create(userId, productId);
            when(spyLikeProductRepository.findByUserIdAndProductId(userId, productId))
                    .thenReturn(Optional.of(existingLike));

            // act
            likeProductService.likeProduct(userId, productId);
            likeProductService.likeProduct(userId, productId);

            // assert
            verify(spyLikeProductRepository, times(2)).findByUserIdAndProductId(1L, 100L);
            verify(spyLikeProductRepository, never()).save(any());
        }
    }


    @DisplayName("좋아요를 취소할 때, ")
    @Nested
    class UnlikeProduct {
        @DisplayName("존재하는 좋아요를 취소하면 삭제된다. (Happy Path)")
        @Test
        void should_deleteLikeProduct_when_likeExists() {
            // arrange
            Long userId = 1L;
            Long productId = 100L;
            LikeProduct likeProduct = LikeProduct.create(userId, productId);
            when(spyLikeProductRepository.findByUserIdAndProductId(userId, productId))
                    .thenReturn(Optional.of(likeProduct));

            // act
            likeProductService.unlikeProduct(userId, productId);

            // assert
            verify(spyLikeProductRepository).findByUserIdAndProductId(1L, 100L);
            assertThat(likeProduct.getDeletedAt()).isNotNull();
        }

        @DisplayName("존재하지 않는 좋아요를 취소해도 예외가 발생하지 않는다. (Edge Case)")
        @Test
        void should_notThrowException_when_likeNotFound() {
            // arrange
            Long userId = 1L;
            Long productId = 100L;
            when(spyLikeProductRepository.findByUserIdAndProductId(userId, productId))
                    .thenReturn(Optional.empty());

            // act & assert
            likeProductService.unlikeProduct(userId, productId);
            verify(spyLikeProductRepository).findByUserIdAndProductId(1L, 100L);
            // 예외가 발생하지 않아야 함
        }

        @DisplayName("이미 삭제된 좋아요를 다시 취소해도 멱등하게 동작한다. (Idempotency)")
        @Test
        void should_beIdempotent_when_unlikeDeletedLike() {
            // arrange
            Long userId = 1L;
            Long productId = 100L;
            LikeProduct deletedLike = LikeProduct.create(userId, productId);
            deletedLike.delete();
            when(spyLikeProductRepository.findByUserIdAndProductId(userId, productId))
                    .thenReturn(Optional.of(deletedLike));

            // act
            likeProductService.unlikeProduct(userId, productId);

            // assert
            verify(spyLikeProductRepository).findByUserIdAndProductId(1L, 100L);
            // delete는 멱등하게 동작하므로 deletedAt이 그대로 유지되어야 함
            assertThat(deletedLike.getDeletedAt()).isNotNull();
        }
    }

    @DisplayName("좋아요 토글을 할 때, ")
    @Nested
    class ToggleLike {
        @DisplayName("좋아요가 없으면 등록하고, 있으면 취소한다. (Toggle)")
        @Test
        void should_toggleLike_when_likeAndUnlike() {
            // arrange
            Long userId = 1L;
            Long productId = 100L;
            when(spyLikeProductRepository.findByUserIdAndProductId(userId, productId))
                    .thenReturn(Optional.empty())
                    .thenReturn(Optional.of(LikeProduct.create(userId, productId)));

            // act - 첫 번째 호출: 좋아요 등록
            likeProductService.likeProduct(userId, productId);
            // 두 번째 호출: 좋아요 취소
            likeProductService.unlikeProduct(userId, productId);

            // assert
            verify(spyLikeProductRepository, times(2)).findByUserIdAndProductId(1L, 100L);
            verify(spyLikeProductRepository, times(1)).save(any(LikeProduct.class));
        }
    }

    @DisplayName("좋아요한 상품 목록을 조회할 때, ")
    @Nested
    class GetLikedProducts {
        @DisplayName("좋아요한 상품이 있으면 목록을 반환한다. (Happy Path)")
        @Test
        void should_returnLikedProducts_when_likesExist() {
            // arrange
            Long userId = 1L;
            Pageable pageable = PageRequest.of(0, 20);
            List<LikeProduct> likedProducts = List.of(
                    LikeProduct.create(userId, 100L),
                    LikeProduct.create(userId, 200L)
            );
            Page<LikeProduct> productPage = new PageImpl<>(likedProducts, pageable, 2);
            when(spyLikeProductRepository.getLikeProductsByUserIdAndDeletedAtIsNull(userId, pageable))
                    .thenReturn(productPage);

            // act
            Page<LikeProduct> result = likeProductService.getLikedProducts(userId, pageable);

            // assert
            verify(spyLikeProductRepository).getLikeProductsByUserIdAndDeletedAtIsNull(1L, Pageable.ofSize(20));
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getTotalElements()).isEqualTo(2);
        }

        @DisplayName("좋아요한 상품이 없으면 빈 목록을 반환한다. (Edge Case)")
        @Test
        void should_returnEmptyList_when_noLikes() {
            // arrange
            Long userId = 1L;
            Pageable pageable = PageRequest.of(0, 20);
            Page<LikeProduct> emptyPage = new PageImpl<>(List.of(), pageable, 0);
            when(spyLikeProductRepository.getLikeProductsByUserIdAndDeletedAtIsNull(userId, pageable))
                    .thenReturn(emptyPage);

            // act
            Page<LikeProduct> result = likeProductService.getLikedProducts(userId, pageable);

            // assert
            verify(spyLikeProductRepository).getLikeProductsByUserIdAndDeletedAtIsNull(1L, Pageable.ofSize(20));
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(0);
        }
    }
}
