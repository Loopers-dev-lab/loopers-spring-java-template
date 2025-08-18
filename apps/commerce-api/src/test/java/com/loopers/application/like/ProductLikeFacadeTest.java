package com.loopers.application.like;

import com.loopers.domain.brand.BrandFixture;
import com.loopers.domain.brand.BrandModel;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.like.fixture.ProductLikeFixture;
import com.loopers.domain.like.product.ProductLikeModel;
import com.loopers.domain.like.product.ProductLikeRepository;
import com.loopers.domain.like.product.ProductLikeService;
import com.loopers.domain.product.ProductFixture;
import com.loopers.domain.product.ProductModel;
import com.loopers.domain.product.ProductRepository;
import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("상품 좋아요 Facade 테스트")
class ProductLikeFacadeTest {

    @Mock
    private ProductLikeRepository productLikeRepository;
    
    @Mock
    private ProductRepository productRepository;
    
    @Mock
    private BrandRepository brandRepository;
    
    @Mock
    private ProductLikeService productLikeService;

    @InjectMocks
    private ProductLikeFacade productLikeFacade;

    private final Long userId = 1L;
    private final Long productId = 100L;

    @Nested
    @DisplayName("상품 좋아요 등록 테스트")
    class AddProductLikeTest {

        @DisplayName("새로운 상품 좋아요를 등록하고 상품 likeCount를 업데이트한다")
        @Test
        void addProductLike_createsLikeAndUpdatesProductCount() {
            // arrange
            ProductModel product = ProductFixture.createProductWithLikeCount(new BigDecimal("5"));
            ProductLikeModel newLike = ProductLikeFixture.createProductLikeModel(userId, productId);

            given(productRepository.findByIdForUpdate(productId))
                    .willReturn(Optional.of(product));
            given(productLikeRepository.existsByUserIdAndProductId(userId, productId))
                    .willReturn(false);
            given(productLikeService.addLike(product, userId))
                    .willReturn(newLike);
            given(productLikeRepository.save(any(ProductLikeModel.class)))
                    .willReturn(newLike);

            // act
            ProductLikeModel result = productLikeFacade.addProductLike(userId, productId);

            // assert
            assertAll(
                    () -> assertThat(result).isEqualTo(newLike),
                    () -> then(productRepository).should().findByIdForUpdate(productId),
                    () -> then(productLikeService).should().addLike(product, userId),
                    () -> then(productLikeRepository).should().save(any(ProductLikeModel.class)),
                    () -> then(productRepository).should().save(product)
            );
        }

        @DisplayName("이미 존재하는 상품 좋아요는 중복 등록되지 않는다")
        @Test
        void addProductLike_whenAlreadyExists_returnsExisting() {
            // arrange
            ProductModel product = ProductFixture.createProductWithLikeCount(new BigDecimal("5"));
            ProductLikeModel existingLike = ProductLikeModel.create(userId, productId);
            
            given(productRepository.findByIdForUpdate(productId))
                    .willReturn(Optional.of(product));
            given(productLikeRepository.existsByUserIdAndProductId(userId, productId))
                    .willReturn(true);
            given(productLikeRepository.findByUserIdAndProductId(userId, productId))
                    .willReturn(Optional.of(existingLike));

            // act
            ProductLikeModel result = productLikeFacade.addProductLike(userId, productId);

            // assert
            assertAll(
                    () -> assertThat(result).isEqualTo(existingLike),
                    () -> then(productRepository).should().findByIdForUpdate(productId),
                    () -> then(productLikeService).should(never()).addLike(any(), any()),
                    () -> then(productRepository).should(never()).save(any())
            );
        }

        @DisplayName("존재하지 않는 상품에 좋아요 등록 시 예외가 발생한다")
        @Test
        void addProductLike_whenProductNotExists_throwsException() {
            // arrange
            given(productRepository.findByIdForUpdate(productId))
                    .willReturn(Optional.empty());

            // act & assert
            assertThatThrownBy(() -> productLikeFacade.addProductLike(userId, productId))
                    .isInstanceOf(CoreException.class)
                    .hasMessage("상품을 찾을 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("상품 좋아요 취소 테스트") 
    class RemoveProductLikeTest {

        @DisplayName("존재하는 상품 좋아요를 취소하고 상품 likeCount를 업데이트한다")
        @Test
        void removeProductLike_deletesLikeAndUpdatesProductCount() {
            // arrange
            ProductModel product = ProductFixture.createProductWithLikeCount(new BigDecimal("5"));
            ProductLikeModel existingLike = ProductLikeModel.create(userId, productId);
            
            given(productLikeRepository.findByUserIdAndProductId(userId, productId))
                    .willReturn(Optional.of(existingLike));
            given(productRepository.findById(productId))
                    .willReturn(Optional.of(product));

            // act
            productLikeFacade.removeProductLike(userId, productId);

            // assert
            then(productRepository).should().findById(productId);
            then(productLikeService).should().removeLike(product, existingLike);
            then(productLikeRepository).should().delete(existingLike);
            then(productRepository).should().save(product);
        }

        @DisplayName("존재하지 않는 상품 좋아요 취소는 아무 동작하지 않는다")
        @Test
        void removeProductLike_whenNotExists_doesNothing() {
            // arrange
            given(productLikeRepository.findByUserIdAndProductId(userId, productId))
                    .willReturn(Optional.empty());

            // act
            productLikeFacade.removeProductLike(userId, productId);

            // assert
            then(productRepository).should(never()).findById(any());
            then(productLikeService).should(never()).removeLike(any(), any());
            then(productRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("상품 좋아요 토글 테스트")
    class ToggleProductLikeTest {

        @DisplayName("상품 좋아요가 없을 때 토글하면 등록하고 상품 likeCount를 증가시킨다")
        @Test
        void toggleLike_whenNotExists_addsLikeAndIncrementsCount() {
            // arrange
            ProductModel product = ProductFixture.createProductWithLikeCount(new BigDecimal("3"));
            ProductLikeModel newLike = ProductLikeModel.create(userId, productId);
            ProductLikeService.LikeToggleResult toggleResult = ProductLikeService.LikeToggleResult.added(newLike);
            
            given(productRepository.findById(productId))
                    .willReturn(Optional.of(product));
            given(productLikeRepository.findByUserIdAndProductId(userId, productId))
                    .willReturn(Optional.empty());
            given(productLikeService.toggleLike(product, userId, null))
                    .willReturn(toggleResult);

            // act
            productLikeFacade.toggleLike(userId, productId);

            // assert
            then(productRepository).should().findById(productId);
            then(productLikeService).should().toggleLike(product, userId, null);
            then(productLikeRepository).should().save(newLike);
            then(productLikeRepository).should(never()).delete(any());
            then(productRepository).should().save(product);
        }

        @DisplayName("상품 좋아요가 있을 때 토글하면 취소하고 상품 likeCount를 감소시킨다")
        @Test
        void toggleLike_whenExists_removesLikeAndDecrementsCount() {
            // arrange
            ProductModel product = ProductFixture.createProductWithLikeCount(new BigDecimal("5"));
            ProductLikeModel existingLike = ProductLikeModel.create(userId, productId);
            ProductLikeService.LikeToggleResult toggleResult = ProductLikeService.LikeToggleResult.removed(existingLike);
            
            given(productRepository.findById(productId))
                    .willReturn(Optional.of(product));
            given(productLikeRepository.findByUserIdAndProductId(userId, productId))
                    .willReturn(Optional.of(existingLike));
            given(productLikeService.toggleLike(product, userId, existingLike))
                    .willReturn(toggleResult);

            // act
            productLikeFacade.toggleLike(userId, productId);

            // assert
            then(productRepository).should().findById(productId);
            then(productLikeService).should().toggleLike(product, userId, existingLike);
            then(productLikeRepository).should().delete(existingLike);
            then(productLikeRepository).should(never()).save(any());
            then(productRepository).should().save(product);
        }
    }

    @Nested
    @DisplayName("상품 좋아요 상태 확인 테스트")
    class IsProductLikedTest {

        @DisplayName("상품 좋아요가 있으면 true를 반환한다")
        @Test
        void isProductLiked_whenExists_returnsTrue() {
            // arrange
            given(productLikeRepository.existsByUserIdAndProductId(userId, productId))
                    .willReturn(true);

            // act
            boolean result = productLikeFacade.isProductLiked(userId, productId);

            // assert
            assertThat(result).isTrue();
            then(productLikeRepository).should().existsByUserIdAndProductId(userId, productId);
        }

        @DisplayName("상품 좋아요가 없으면 false를 반환한다")
        @Test
        void isProductLiked_whenNotExists_returnsFalse() {
            // arrange
            given(productLikeRepository.existsByUserIdAndProductId(userId, productId))
                    .willReturn(false);

            // act
            boolean result = productLikeFacade.isProductLiked(userId, productId);

            // assert
            assertThat(result).isFalse();
            then(productLikeRepository).should().existsByUserIdAndProductId(userId, productId);
        }
    }

    @Nested
    @DisplayName("상품 좋아요 멱등성 및 통합 테스트")
    class IdempotencyAndIntegrationTest {

        @DisplayName("상품 좋아요 전체 생명주기에서 likeCount가 정확히 관리된다")
        @Test
        void productLikeLifecycle_maintainsCorrectCount() {
            // arrange
            ProductModel product = ProductFixture.createProductWithLikeCount(new BigDecimal("10"));
            ProductLikeModel like = ProductLikeModel.create(userId, productId);
            
            given(productRepository.findByIdForUpdate(productId))
                    .willReturn(Optional.of(product));
            given(productRepository.findById(productId))
                    .willReturn(Optional.of(product));
            
            given(productLikeRepository.existsByUserIdAndProductId(userId, productId))
                    .willReturn(false, true); 
            given(productLikeService.addLike(product, userId))
                    .willReturn(like);
            given(productLikeRepository.save(any(ProductLikeModel.class)))
                    .willReturn(like);
            
            given(productLikeRepository.findByUserIdAndProductId(userId, productId))
                    .willReturn(Optional.of(like));

            // act & assert
            // 1. 첫 번째 등록
            ProductLikeModel result1 = productLikeFacade.addProductLike(userId, productId);
            assertThat(result1).isEqualTo(like);

            // 2. 중복 등록 시도 (멱등성)
            ProductLikeModel result2 = productLikeFacade.addProductLike(userId, productId);
            assertThat(result2).isEqualTo(like);

            // 3. 취소
            productLikeFacade.removeProductLike(userId, productId);

            // verify: 실제 등록은 한 번만, 상품은 등록과 취소 시 각각 저장
            then(productLikeService).should(times(1)).addLike(product, userId);
            then(productLikeService).should(times(1)).removeLike(product, like);
            then(productRepository).should(times(2)).save(product); // 등록과 취소 시
        }
    }

    @Nested
    @DisplayName("좋아요한 상품 목록 조회 테스트")
    class GetLikedProductsTest {

        @DisplayName("사용자의 좋아요한 상품 목록을 최신 순으로 조회한다")
        @Test
        void getUserLikedProducts_returnsLikedProductsInDescOrder() {
            // Arrange
            int page = 0;
            int size = 10;
            Pageable pageable = PageRequest.of(page, size);
            
            ProductLikeModel like1 = ProductLikeModel.create(userId, 100L);
            ProductLikeModel like2 = ProductLikeModel.create(userId, 200L);
            Page<ProductLikeModel> likePage = new PageImpl<>(List.of(like1, like2), pageable, 2);
            
            ProductModel product1 = ProductFixture.createProductWithBrandId(100L, 1L);
            ProductModel product2 = ProductFixture.createProductWithBrandId(200L, 2L);
            
            BrandModel brand1 = BrandFixture.createBrand("브랜드1");
            BrandModel brand2 = BrandFixture.createBrand("브랜드2");
            
            ProductLikeCommand.Request.GetLikedProducts request = 
                    new ProductLikeCommand.Request.GetLikedProducts(userId, page, size);
            
            given(productLikeRepository.findByUserIdOrderByLikedAtDesc(userId, pageable))
                    .willReturn(likePage);
            given(productRepository.findByIdIn(List.of(100L, 200L)))
                    .willReturn(List.of(product1, product2));
            given(brandRepository.findByIdIn(List.of(1L, 2L)))
                    .willReturn(List.of(brand1, brand2));

            // Act
            ProductLikeCommand.LikedProductsData result = productLikeFacade.getUserLikedProducts(request);

            // Assert
            assertAll(
                    () -> assertThat(result.productLikes()).isEqualTo(likePage),
                    () -> assertThat(result.likedProductItems()).hasSize(2),
                    () -> assertThat(result.likedProductItems().get(0).productId()).isEqualTo(100L),
                    () -> assertThat(result.likedProductItems().get(0).brandName()).isEqualTo("브랜드1"),
                    () -> assertThat(result.likedProductItems().get(1).productId()).isEqualTo(200L),
                    () -> assertThat(result.likedProductItems().get(1).brandName()).isEqualTo("브랜드2")
            );

            then(productLikeRepository).should().findByUserIdOrderByLikedAtDesc(userId, pageable);
            then(productRepository).should().findByIdIn(List.of(100L, 200L));
            then(brandRepository).should().findByIdIn(List.of(1L, 2L));
        }

        @DisplayName("좋아요한 상품이 없으면 빈 목록을 반환한다")
        @Test
        void getUserLikedProducts_whenNoLikes_returnsEmptyList() {
            // Arrange
            int page = 0;
            int size = 10;
            Pageable pageable = PageRequest.of(page, size);
            Page<ProductLikeModel> emptyPage = new PageImpl<>(List.of(), pageable, 0);
            
            ProductLikeCommand.Request.GetLikedProducts request = 
                    new ProductLikeCommand.Request.GetLikedProducts(userId, page, size);
            
            given(productLikeRepository.findByUserIdOrderByLikedAtDesc(userId, pageable))
                    .willReturn(emptyPage);

            // Act
            ProductLikeCommand.LikedProductsData result = productLikeFacade.getUserLikedProducts(request);

            // Assert
            assertAll(
                    () -> assertThat(result.productLikes()).isEqualTo(emptyPage),
                    () -> assertThat(result.likedProductItems()).isEmpty()
            );

            then(productRepository).should(never()).findByIdIn(anyList());
            then(brandRepository).should(never()).findByIdIn(anyList());
        }

        @DisplayName("삭제된 상품은 좋아요 목록에서 제외된다")
        @Test
        void getUserLikedProducts_filtersOutDeletedProducts() {
            // Arrange
            int page = 0;
            int size = 10;
            Pageable pageable = PageRequest.of(page, size);
            
            ProductLikeModel like1 = ProductLikeModel.create(userId, 100L);
            ProductLikeModel like2 = ProductLikeModel.create(userId, 200L); // 삭제된 상품
            Page<ProductLikeModel> likePage = new PageImpl<>(List.of(like1, like2), pageable, 2);
            
            ProductModel product1 = ProductFixture.createProductWithBrandId(100L, 1L);
            // product2는 삭제되어 조회되지 않음
            
            BrandModel brand1 = BrandFixture.createBrand("브랜드1");
            
            ProductLikeCommand.Request.GetLikedProducts request = 
                    new ProductLikeCommand.Request.GetLikedProducts(userId, page, size);
            
            given(productLikeRepository.findByUserIdOrderByLikedAtDesc(userId, pageable))
                    .willReturn(likePage);
            given(productRepository.findByIdIn(List.of(100L, 200L)))
                    .willReturn(List.of(product1)); // product2는 없음
            given(brandRepository.findByIdIn(List.of(1L)))
                    .willReturn(List.of(brand1));

            // Act
            ProductLikeCommand.LikedProductsData result = productLikeFacade.getUserLikedProducts(request);

            // Assert
            assertAll(
                    () -> assertThat(result.productLikes()).isEqualTo(likePage),
                    () -> assertThat(result.likedProductItems()).hasSize(1), // 삭제된 상품 제외
                    () -> assertThat(result.likedProductItems().get(0).productId()).isEqualTo(100L)
            );
        }
    }
}
