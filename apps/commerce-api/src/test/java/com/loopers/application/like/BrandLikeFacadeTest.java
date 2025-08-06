package com.loopers.application.like;

import com.loopers.application.brand.BrandFacade;
import com.loopers.domain.brand.BrandModel;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.brand.BrandFixture;
import com.loopers.domain.like.BrandLikeModel;
import com.loopers.domain.like.BrandLikeRepository;
import com.loopers.domain.like.BrandLikeService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("브랜드 좋아요 Facade 테스트")
class BrandLikeFacadeTest {

    @Mock
    private BrandLikeRepository brandLikeRepository;
    
    @Mock
    private BrandRepository brandRepository;
    
    @Mock
    private BrandFacade brandFacde;
    
    @Mock
    private BrandLikeService brandLikeService;

    @InjectMocks
    private BrandLikeFacade brandLikeFacade;

    private final Long userId = 1L;
    private final Long brandId = 100L;

    @Nested
    @DisplayName("브랜드 좋아요 등록 테스트")
    class AddBrandLikeTest {

        @DisplayName("새로운 브랜드 좋아요를 등록하고 브랜드 likeCount를 업데이트한다")
        @Test
        void addBrandLike_createsLikeAndUpdatesBrandCount() {
            // arrange
            BrandModel brand = BrandFixture.createBrandWithLikeCount(new BigDecimal("5"));
            BrandLikeModel newLike = BrandLikeModel.create(userId, brandId);
            
            given(brandLikeRepository.existsByUserIdAndBrandId(userId, brandId))
                    .willReturn(false);
            given(brandFacde.getByIdOrThrow(brandId))
                    .willReturn(brand);
            given(brandLikeService.addLike(brand, userId))
                    .willReturn(newLike);
            given(brandLikeRepository.save(newLike))
                    .willReturn(newLike);

            // act
            BrandLikeModel result = brandLikeFacade.addBrandLike(userId, brandId);

            // assert
            assertAll(
                    () -> assertThat(result).isEqualTo(newLike),
                    () -> then(brandFacde).should().getByIdOrThrow(brandId),
                    () -> then(brandLikeService).should().addLike(brand, userId),
                    () -> then(brandLikeRepository).should().save(newLike),
                    () -> then(brandRepository).should().save(brand)
            );
        }

        @DisplayName("이미 존재하는 브랜드 좋아요는 중복 등록되지 않는다")
        @Test
        void addBrandLike_whenAlreadyExists_returnsExisting() {
            // arrange
            BrandLikeModel existingLike = BrandLikeModel.create(userId, brandId);
            given(brandLikeRepository.existsByUserIdAndBrandId(userId, brandId))
                    .willReturn(true);
            given(brandLikeRepository.findByUserIdAndBrandId(userId, brandId))
                    .willReturn(Optional.of(existingLike));

            // act
            BrandLikeModel result = brandLikeFacade.addBrandLike(userId, brandId);

            // assert
            assertAll(
                    () -> assertThat(result).isEqualTo(existingLike),
                    () -> then(brandFacde).should(never()).getByIdOrThrow(any()),
                    () -> then(brandLikeService).should(never()).addLike(any(), any()),
                    () -> then(brandRepository).should(never()).save(any())
            );
        }

        @DisplayName("존재하지 않는 브랜드에 좋아요 등록 시 예외가 발생한다")
        @Test
        void addBrandLike_whenBrandNotExists_throwsException() {
            // arrange
            given(brandLikeRepository.existsByUserIdAndBrandId(userId, brandId))
                    .willReturn(false);
            given(brandFacde.getByIdOrThrow(brandId))
                    .willThrow(new CoreException(ErrorType.BAD_REQUEST, "존재하지 않는 브랜드 id"));

            // act & assert
            assertThatThrownBy(() -> brandLikeFacade.addBrandLike(userId, brandId))
                    .isInstanceOf(CoreException.class)
                    .hasMessage("존재하지 않는 브랜드 id");
        }
    }

    @Nested
    @DisplayName("브랜드 좋아요 취소 테스트") 
    class RemoveBrandLikeTest {

        @DisplayName("존재하는 브랜드 좋아요를 취소하고 브랜드 likeCount를 업데이트한다")
        @Test
        void removeBrandLike_deletesLikeAndUpdatesBrandCount() {
            // arrange
            BrandModel brand = BrandFixture.createBrandWithLikeCount(new BigDecimal("5"));
            BrandLikeModel existingLike = BrandLikeModel.create(userId, brandId);
            
            given(brandLikeRepository.findByUserIdAndBrandId(userId, brandId))
                    .willReturn(Optional.of(existingLike));
            given(brandFacde.getByIdOrThrow(brandId))
                    .willReturn(brand);

            // act
            brandLikeFacade.removeBrandLike(userId, brandId);

            // assert
            then(brandFacde).should().getByIdOrThrow(brandId);
            then(brandLikeService).should().removeLike(brand, existingLike);
            then(brandLikeRepository).should().delete(existingLike);
            then(brandRepository).should().save(brand);
        }

        @DisplayName("존재하지 않는 브랜드 좋아요 취소는 아무 동작하지 않는다")
        @Test
        void removeBrandLike_whenNotExists_doesNothing() {
            // arrange
            given(brandLikeRepository.findByUserIdAndBrandId(userId, brandId))
                    .willReturn(Optional.empty());

            // act
            brandLikeFacade.removeBrandLike(userId, brandId);

            // assert
            then(brandFacde).should(never()).getByIdOrThrow(any());
            then(brandLikeService).should(never()).removeLike(any(), any());
            then(brandRepository).should(never()).save(any());
        }
    }

    @Nested
    @DisplayName("브랜드 좋아요 토글 테스트")
    class ToggleBrandLikeTest {

        @DisplayName("브랜드 좋아요가 없을 때 토글하면 등록하고 브랜드 likeCount를 증가시킨다")
        @Test
        void toggleBrandLike_whenNotExists_addsLikeAndIncrementsCount() {
            // arrange
            BrandModel brand = BrandFixture.createBrandWithLikeCount(new BigDecimal("3"));
            BrandLikeModel newLike = BrandLikeModel.create(userId, brandId);
            BrandLikeService.LikeToggleResult toggleResult = BrandLikeService.LikeToggleResult.added(newLike);
            
            given(brandFacde.getByIdOrThrow(brandId))
                    .willReturn(brand);
            given(brandLikeRepository.findByUserIdAndBrandId(userId, brandId))
                    .willReturn(Optional.empty());
            given(brandLikeService.toggleLike(brand, userId, null))
                    .willReturn(toggleResult);

            // act
            brandLikeFacade.toggleBrandLike(userId, brandId);

            // assert
            then(brandFacde).should().getByIdOrThrow(brandId);
            then(brandLikeService).should().toggleLike(brand, userId, null);
            then(brandLikeRepository).should().save(newLike);
            then(brandLikeRepository).should(never()).delete(any());
            then(brandRepository).should().save(brand);
        }

        @DisplayName("브랜드 좋아요가 있을 때 토글하면 취소하고 브랜드 likeCount를 감소시킨다")
        @Test
        void toggleBrandLike_whenExists_removesLikeAndDecrementsCount() {
            // arrange
            BrandModel brand = BrandFixture.createBrandWithLikeCount(new BigDecimal("5"));
            BrandLikeModel existingLike = BrandLikeModel.create(userId, brandId);
            BrandLikeService.LikeToggleResult toggleResult = BrandLikeService.LikeToggleResult.removed(existingLike);
            
            given(brandFacde.getByIdOrThrow(brandId))
                    .willReturn(brand);
            given(brandLikeRepository.findByUserIdAndBrandId(userId, brandId))
                    .willReturn(Optional.of(existingLike));
            given(brandLikeService.toggleLike(brand, userId, existingLike))
                    .willReturn(toggleResult);

            // act
            brandLikeFacade.toggleBrandLike(userId, brandId);

            // assert
            then(brandFacde).should().getByIdOrThrow(brandId);
            then(brandLikeService).should().toggleLike(brand, userId, existingLike);
            then(brandLikeRepository).should().delete(existingLike);
            then(brandLikeRepository).should(never()).save(any());
            then(brandRepository).should().save(brand);
        }
    }

    @Nested
    @DisplayName("브랜드 좋아요 상태 확인 테스트")
    class IsBrandLikedTest {

        @DisplayName("브랜드 좋아요가 있으면 true를 반환한다")
        @Test
        void isBrandLiked_whenExists_returnsTrue() {
            // arrange
            given(brandLikeRepository.existsByUserIdAndBrandId(userId, brandId))
                    .willReturn(true);

            // act
            boolean result = brandLikeFacade.isBrandLiked(userId, brandId);

            // assert
            assertThat(result).isTrue();
            then(brandLikeRepository).should().existsByUserIdAndBrandId(userId, brandId);
        }

        @DisplayName("브랜드 좋아요가 없으면 false를 반환한다")
        @Test
        void isBrandLiked_whenNotExists_returnsFalse() {
            // arrange
            given(brandLikeRepository.existsByUserIdAndBrandId(userId, brandId))
                    .willReturn(false);

            // act
            boolean result = brandLikeFacade.isBrandLiked(userId, brandId);

            // assert
            assertThat(result).isFalse();
            then(brandLikeRepository).should().existsByUserIdAndBrandId(userId, brandId);
        }
    }

    @Nested
    @DisplayName("브랜드 좋아요 멱등성 및 통합 테스트")
    class IdempotencyAndIntegrationTest {

        @DisplayName("브랜드 좋아요 전체 생명주기에서 likeCount가 정확히 관리된다")
        @Test
        void brandLikeLifecycle_maintainsCorrectCount() {
            // arrange
            BrandModel brand = BrandFixture.createBrandWithLikeCount(new BigDecimal("10"));
            BrandLikeModel like = BrandLikeModel.create(userId, brandId);
            
            // Mock 설정: 등록 -> 중복 등록 시도 -> 취소 순서
            given(brandFacde.getByIdOrThrow(brandId))
                    .willReturn(brand);
            
            // 첫 번째 등록
            given(brandLikeRepository.existsByUserIdAndBrandId(userId, brandId))
                    .willReturn(false, true); // 첫 번째는 false, 두 번째는 true
            given(brandLikeService.addLike(brand, userId))
                    .willReturn(like);
            
            // 중복 등록 시도
            given(brandLikeRepository.findByUserIdAndBrandId(userId, brandId))
                    .willReturn(Optional.of(like));
            
            // 취소
            given(brandLikeRepository.findByUserIdAndBrandId(userId, brandId))
                    .willReturn(Optional.of(like));

            // act & assert
            // 1. 첫 번째 등록
            BrandLikeModel result1 = brandLikeFacade.addBrandLike(userId, brandId);
            assertThat(result1).isEqualTo(like);

            // 2. 중복 등록 시도 (멱등성)
            BrandLikeModel result2 = brandLikeFacade.addBrandLike(userId, brandId);
            assertThat(result2).isEqualTo(like);

            // 3. 취소
            brandLikeFacade.removeBrandLike(userId, brandId);

            // verify: 실제 등록은 한 번만, 브랜드는 등록과 취소 시 각각 저장
            then(brandLikeService).should(times(1)).addLike(brand, userId);
            then(brandLikeService).should(times(1)).removeLike(brand, like);
            then(brandRepository).should(times(2)).save(brand); // 등록과 취소 시
        }
    }
}
