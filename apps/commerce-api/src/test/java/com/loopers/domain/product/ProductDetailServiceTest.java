package com.loopers.domain.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.brand.BrandStatus;
import com.loopers.domain.like.LikeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@DisplayName("ProductDetailService 단위 테스트")
class ProductDetailServiceTest {

    private Product createProduct(Long id, Long brandId, ProductStatus status) {
        return Product.reconstitute(
                id,
                "테스트 상품",
                "설명",
                new BigDecimal("10000"),
                10,
                "image.jpg",
                brandId,
                status,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private Brand createBrand(Long id, BrandStatus status) {
        return Brand.reconstitute(
                id,
                "브랜드명",
                "브랜드 설명",
                "brand.jpg",
                status,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private ProductDetailService newService(ProductRepository productRepository,
                                            BrandRepository brandRepository,
                                            LikeService likeService) {
        return new ProductDetailService(productRepository, brandRepository, likeService);
    }

    @Nested
    @DisplayName("정상 플로우")
    class SuccessCases {
        @Test
        @DisplayName("상품 + 브랜드 + 좋아요 메타를 조합해 상세를 반환한다 (사용자가 좋아요한 경우)")
        void getProductDetail_success_liked() {
            // given
            ProductRepository productRepository = mock(ProductRepository.class);
            BrandRepository brandRepository = mock(BrandRepository.class);
            LikeService likeService = mock(LikeService.class);

            Long productId = 100L;
            Long brandId = 10L;
            String userId = "user-1";

            when(productRepository.findById(productId))
                    .thenReturn(Optional.of(createProduct(productId, brandId, ProductStatus.ACTIVE)));
            when(brandRepository.findById(brandId))
                    .thenReturn(Optional.of(createBrand(brandId, BrandStatus.ACTIVE)));
            when(likeService.getLikeCount(productId)).thenReturn(5);
            when(likeService.isLiked(userId, productId)).thenReturn(true);

            ProductDetailService service = newService(productRepository, brandRepository, likeService);

            // when
            ProductDetail detail = service.getProductDetail(productId, userId);

            // then
            assertThat(detail.product().getId()).isEqualTo(productId);
            assertThat(detail.brand().getId()).isEqualTo(brandId);
            assertThat(detail.likeCount()).isEqualTo(5);
            assertThat(detail.likedByUser()).isTrue();

            verify(productRepository, times(1)).findById(productId);
            verify(brandRepository, times(1)).findById(brandId);
            verify(likeService, times(1)).getLikeCount(productId);
            verify(likeService, times(1)).isLiked(userId, productId);
        }

        @Test
        @DisplayName("userId가 null이면 likedByUser=false로 반환한다")
        void getProductDetail_userIdNull() {
            // given
            ProductRepository productRepository = mock(ProductRepository.class);
            BrandRepository brandRepository = mock(BrandRepository.class);
            LikeService likeService = mock(LikeService.class);

            Long productId = 101L;
            Long brandId = 11L;

            when(productRepository.findById(productId))
                    .thenReturn(Optional.of(createProduct(productId, brandId, ProductStatus.ACTIVE)));
            when(brandRepository.findById(brandId))
                    .thenReturn(Optional.of(createBrand(brandId, BrandStatus.ACTIVE)));
            when(likeService.getLikeCount(productId)).thenReturn(0);

            ProductDetailService service = newService(productRepository, brandRepository, likeService);

            // when
            ProductDetail detail = service.getProductDetail(productId, null);

            // then
            assertThat(detail.likeCount()).isEqualTo(0);
            assertThat(detail.likedByUser()).isFalse();
            verify(likeService, never()).isLiked(any(), anyLong());
        }
    }

    @Nested
    @DisplayName("예외 케이스")
    class ExceptionalCases {
        @Test
        @DisplayName("상품이 없으면 CoreException(NOT_FOUND)")
        void productNotFound() {
            // given
            ProductRepository productRepository = mock(ProductRepository.class);
            BrandRepository brandRepository = mock(BrandRepository.class);
            LikeService likeService = mock(LikeService.class);

            Long productId = 200L;
            when(productRepository.findById(productId)).thenReturn(Optional.empty());

            ProductDetailService service = newService(productRepository, brandRepository, likeService);

            // when & then
            assertThatThrownBy(() -> service.getProductDetail(productId, "u"))
                    .isInstanceOf(com.loopers.support.error.CoreException.class)
                    .hasMessageContaining("상품을 찾을 수 없습니다");
        }

        @Test
        @DisplayName("삭제된 상품은 조회 불가 → CoreException(BAD_REQUEST)")
        void productNotViewable_deleted() {
            // given
            ProductRepository productRepository = mock(ProductRepository.class);
            BrandRepository brandRepository = mock(BrandRepository.class);
            LikeService likeService = mock(LikeService.class);

            Long productId = 201L;
            Long brandId = 21L;
            when(productRepository.findById(productId))
                    .thenReturn(Optional.of(createProduct(productId, brandId, ProductStatus.DELETED)));

            ProductDetailService service = newService(productRepository, brandRepository, likeService);

            // when & then
            assertThatThrownBy(() -> service.getProductDetail(productId, "u"))
                    .isInstanceOf(com.loopers.support.error.CoreException.class)
                    .hasMessageContaining("조회할 수 없는 상품");
            verify(brandRepository, never()).findById(anyLong());
        }

        @Test
        @DisplayName("브랜드가 없으면 CoreException(NOT_FOUND)")
        void brandNotFound() {
            // given
            ProductRepository productRepository = mock(ProductRepository.class);
            BrandRepository brandRepository = mock(BrandRepository.class);
            LikeService likeService = mock(LikeService.class);

            Long productId = 202L;
            Long brandId = 22L;

            when(productRepository.findById(productId))
                    .thenReturn(Optional.of(createProduct(productId, brandId, ProductStatus.ACTIVE)));
            when(brandRepository.findById(brandId))
                    .thenReturn(Optional.empty());

            ProductDetailService service = newService(productRepository, brandRepository, likeService);

            // when & then
            assertThatThrownBy(() -> service.getProductDetail(productId, "u"))
                    .isInstanceOf(com.loopers.support.error.CoreException.class)
                    .hasMessageContaining("브랜드를 찾을 수 없습니다");
        }
    }
}
