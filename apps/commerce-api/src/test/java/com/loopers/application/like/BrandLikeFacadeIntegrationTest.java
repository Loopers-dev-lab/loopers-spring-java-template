package com.loopers.application.like;

import com.loopers.domain.brand.BrandFixture;
import com.loopers.domain.brand.BrandModel;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.like.brand.BrandLikeModel;
import com.loopers.domain.like.brand.BrandLikeRepository;
import com.loopers.domain.like.fixture.BrandLikeFixture;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class BrandLikeFacadeIntegrationTest {

    @Autowired
    private BrandLikeFacade brandLikeFacade;
    
    @Autowired
    private BrandRepository brandRepository;
    
    @Autowired
    private BrandLikeRepository brandLikeRepository;
    
    private BrandModel savedBrand;
    private final Long testUserId = 1L;
    
    @BeforeEach
    void setUp() {
        brandLikeRepository.deleteAll();
        brandRepository.deleteAll();
        
        BrandModel brandModel = BrandFixture.createBrandModel();
        savedBrand = brandRepository.save(brandModel);
    }
    
    @Nested
    @DisplayName("브랜드 좋아요 토글 테스트")
    class ToggleBrandLikeTest {
        
        @DisplayName("브랜드 좋아요가 없는 상태에서 토글하면 좋아요가 추가된다")
        @Test
        void toggleBrandLike_addLike_success() {
            // arrange
            Long userId = testUserId;
            Long brandId = savedBrand.getId();
            
            // act
            brandLikeFacade.toggleBrandLike(userId, brandId);
            
            // assert
            assertAll(
                    () -> assertThat(brandLikeRepository.existsByUserIdAndBrandId(userId, brandId)).isTrue(),
                    () -> assertThat(brandLikeFacade.isBrandLiked(userId, brandId)).isTrue()
            );
        }
        
        @DisplayName("브랜드 좋아요가 있는 상태에서 토글하면 좋아요가 제거된다")
        @Test
        void toggleBrandLike_removeLike_success() {
            // arrange
            Long userId = testUserId;
            Long brandId = savedBrand.getId();
            BrandLikeModel existingLike = BrandLikeFixture.createBrandLikeModel(userId, brandId);
            brandLikeRepository.save(existingLike);
            savedBrand.incrementLikeCount();
            brandRepository.save(savedBrand);
            
            // act
            brandLikeFacade.toggleBrandLike(userId, brandId);
            
            // assert
            assertAll(
                    () -> assertThat(brandLikeRepository.existsByUserIdAndBrandId(userId, brandId)).isFalse(),
                    () -> assertThat(brandLikeFacade.isBrandLiked(userId, brandId)).isFalse()
            );
        }
        
        @DisplayName("존재하지 않는 브랜드에 좋아요 토글 시 예외가 발생한다")
        @Test
        void toggleBrandLike_brandNotFound_throwsException() {
            // arrange
            Long userId = testUserId;
            Long invalidBrandId = 999L;
            
            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                brandLikeFacade.toggleBrandLike(userId, invalidBrandId);
            });
            
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
    
    @Nested
    @DisplayName("브랜드 좋아요 추가 테스트")
    class AddBrandLikeTest {
        
        @DisplayName("브랜드 좋아요를 정상적으로 추가할 수 있다")
        @Test
        void addBrandLike_success() {
            // arrange
            Long userId = testUserId;
            Long brandId = savedBrand.getId();
            
            // act
            BrandLikeModel result = brandLikeFacade.addBrandLike(userId, brandId);
            
            // assert
            assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.getUserId()).isEqualTo(userId),
                    () -> assertThat(result.getBrandId()).isEqualTo(brandId),
                    () -> assertThat(brandLikeRepository.existsByUserIdAndBrandId(userId, brandId)).isTrue(),
                    () -> assertThat(brandLikeFacade.isBrandLiked(userId, brandId)).isTrue()
            );
        }
        
        @DisplayName("이미 좋아요한 브랜드에 좋아요 추가 시 기존 좋아요를 반환한다")
        @Test
        void addBrandLike_alreadyExists_returnsExisting() {
            // arrange
            Long userId = testUserId;
            Long brandId = savedBrand.getId();
            BrandLikeModel existingLike = BrandLikeFixture.createBrandLikeModel(userId, brandId);
            brandLikeRepository.save(existingLike);
            
            // act
            BrandLikeModel result = brandLikeFacade.addBrandLike(userId, brandId);
            
            // assert
            assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.getUserId()).isEqualTo(userId),
                    () -> assertThat(result.getBrandId()).isEqualTo(brandId),
                    () -> assertThat(brandLikeRepository.count()).isEqualTo(1)
            );
        }
        
        @DisplayName("존재하지 않는 브랜드에 좋아요 추가 시 예외가 발생한다")
        @Test
        void addBrandLike_brandNotFound_throwsException() {
            // arrange
            Long userId = testUserId;
            Long invalidBrandId = 999L;
            
            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                brandLikeFacade.addBrandLike(userId, invalidBrandId);
            });
            
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
    
    @Nested
    @DisplayName("브랜드 좋아요 제거 테스트")
    class RemoveBrandLikeTest {
        
        @DisplayName("브랜드 좋아요를 정상적으로 제거할 수 있다")
        @Test
        void removeBrandLike_success() {
            // arrange
            Long userId = testUserId;
            Long brandId = savedBrand.getId();
            BrandLikeModel existingLike = BrandLikeFixture.createBrandLikeModel(userId, brandId);
            brandLikeRepository.save(existingLike);
            savedBrand.incrementLikeCount();
            brandRepository.save(savedBrand);
            
            // act
            brandLikeFacade.removeBrandLike(userId, brandId);
            
            // assert
            assertAll(
                    () -> assertThat(brandLikeRepository.existsByUserIdAndBrandId(userId, brandId)).isFalse(),
                    () -> assertThat(brandLikeFacade.isBrandLiked(userId, brandId)).isFalse()
            );
        }
        
        @DisplayName("좋아요하지 않은 브랜드의 좋아요 제거 시 아무 작업도 하지 않는다")
        @Test
        void removeBrandLike_notExists_noAction() {
            // arrange
            Long userId = testUserId;
            Long brandId = savedBrand.getId();
            
            // act
            brandLikeFacade.removeBrandLike(userId, brandId);
            
            // assert
            assertThat(brandLikeRepository.existsByUserIdAndBrandId(userId, brandId)).isFalse();
        }
        
        @DisplayName("존재하지 않는 브랜드의 좋아요 제거 시 예외가 발생한다")
        @Test
        void removeBrandLike_brandNotFound_throwsException() {
            // arrange
            Long userId = testUserId;
            Long invalidBrandId = 999L;
            BrandLikeModel existingLike = BrandLikeFixture.createBrandLikeModel(userId, invalidBrandId);
            brandLikeRepository.save(existingLike);
            
            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                brandLikeFacade.removeBrandLike(userId, invalidBrandId);
            });
            
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
    
    @Nested
    @DisplayName("브랜드 좋아요 상태 확인 테스트")
    class IsBrandLikedTest {
        
        @DisplayName("좋아요한 브랜드의 상태를 올바르게 확인할 수 있다")
        @Test
        void isBrandLiked_liked_returnsTrue() {
            // arrange
            Long userId = testUserId;
            Long brandId = savedBrand.getId();
            BrandLikeModel existingLike = BrandLikeFixture.createBrandLikeModel(userId, brandId);
            brandLikeRepository.save(existingLike);
            
            // act
            boolean result = brandLikeFacade.isBrandLiked(userId, brandId);
            
            // assert
            assertThat(result).isTrue();
        }
        
        @DisplayName("좋아요하지 않은 브랜드의 상태를 올바르게 확인할 수 있다")
        @Test
        void isBrandLiked_notLiked_returnsFalse() {
            // arrange
            Long userId = testUserId;
            Long brandId = savedBrand.getId();
            
            // act
            boolean result = brandLikeFacade.isBrandLiked(userId, brandId);
            
            // assert
            assertThat(result).isFalse();
        }
        
        @DisplayName("다른 사용자가 좋아요한 브랜드에 대해 올바른 결과를 반환한다")
        @Test
        void isBrandLiked_differentUser_returnsFalse() {
            // arrange
            Long userId1 = testUserId;
            Long userId2 = 2L;
            Long brandId = savedBrand.getId();
            BrandLikeModel existingLike = BrandLikeFixture.createBrandLikeModel(userId1, brandId);
            brandLikeRepository.save(existingLike);
            
            // act
            boolean result = brandLikeFacade.isBrandLiked(userId2, brandId);
            
            // assert
            assertThat(result).isFalse();
        }
    }
    
    @Nested
    @DisplayName("복합 시나리오 테스트")
    class ComplexScenarioTest {
        
        @DisplayName("여러 사용자가 같은 브랜드에 좋아요를 추가할 수 있다")
        @Test
        void multipleLikes_sameBrand() {
            // arrange
            Long userId1 = 1L;
            Long userId2 = 2L;
            Long userId3 = 3L;
            Long brandId = savedBrand.getId();
            
            // act
            brandLikeFacade.addBrandLike(userId1, brandId);
            brandLikeFacade.addBrandLike(userId2, brandId);
            brandLikeFacade.addBrandLike(userId3, brandId);
            
            // assert
            assertAll(
                    () -> assertThat(brandLikeFacade.isBrandLiked(userId1, brandId)).isTrue(),
                    () -> assertThat(brandLikeFacade.isBrandLiked(userId2, brandId)).isTrue(),
                    () -> assertThat(brandLikeFacade.isBrandLiked(userId3, brandId)).isTrue(),
                    () -> assertThat(brandLikeRepository.count()).isEqualTo(3)
            );
        }
        
        @DisplayName("한 사용자가 여러 브랜드에 좋아요를 추가할 수 있다")
        @Test
        void multipleLikes_sameUser() {
            // arrange
            Long userId = testUserId;
            BrandModel brand2 = brandRepository.save(BrandFixture.createBrandModel());
            BrandModel brand3 = brandRepository.save(BrandFixture.createBrandModel());
            
            // act
            brandLikeFacade.addBrandLike(userId, savedBrand.getId());
            brandLikeFacade.addBrandLike(userId, brand2.getId());
            brandLikeFacade.addBrandLike(userId, brand3.getId());
            
            // assert
            assertAll(
                    () -> assertThat(brandLikeFacade.isBrandLiked(userId, savedBrand.getId())).isTrue(),
                    () -> assertThat(brandLikeFacade.isBrandLiked(userId, brand2.getId())).isTrue(),
                    () -> assertThat(brandLikeFacade.isBrandLiked(userId, brand3.getId())).isTrue(),
                    () -> assertThat(brandLikeRepository.count()).isEqualTo(3)
            );
        }
        
        @DisplayName("좋아요 추가 후 토글하면 좋아요가 제거된다")
        @Test
        void addThenToggle_removesLike() {
            // arrange
            Long userId = testUserId;
            Long brandId = savedBrand.getId();
            
            // act
            brandLikeFacade.addBrandLike(userId, brandId);
            assertThat(brandLikeFacade.isBrandLiked(userId, brandId)).isTrue();
            
            brandLikeFacade.toggleBrandLike(userId, brandId);
            
            // assert
            assertThat(brandLikeFacade.isBrandLiked(userId, brandId)).isFalse();
        }
        
        @DisplayName("좋아요 제거 후 토글하면 좋아요가 추가된다")
        @Test
        void removeThenToggle_addsLike() {
            // arrange
            Long userId = testUserId;
            Long brandId = savedBrand.getId();
            BrandLikeModel existingLike = BrandLikeFixture.createBrandLikeModel(userId, brandId);
            brandLikeRepository.save(existingLike);
            savedBrand.incrementLikeCount();
            brandRepository.save(savedBrand);
            
            // act
            brandLikeFacade.removeBrandLike(userId, brandId);
            assertThat(brandLikeFacade.isBrandLiked(userId, brandId)).isFalse();
            
            brandLikeFacade.toggleBrandLike(userId, brandId);
            
            // assert
            assertThat(brandLikeFacade.isBrandLiked(userId, brandId)).isTrue();
        }
    }
}
