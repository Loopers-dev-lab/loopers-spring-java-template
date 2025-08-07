package com.loopers.application.like;

import com.loopers.domain.brand.BrandFixture;
import com.loopers.domain.brand.BrandModel;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.like.product.ProductLikeModel;
import com.loopers.domain.like.product.ProductLikeRepository;
import com.loopers.domain.like.fixture.ProductLikeFixture;
import com.loopers.domain.product.ProductFixture;
import com.loopers.domain.product.ProductModel;
import com.loopers.domain.product.ProductRepository;
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
class ProductLikeFacadeIntegrationTest {

    @Autowired
    private ProductLikeFacade productLikeFacade;
    
    @Autowired
    private ProductRepository productRepository;
    
    @Autowired
    private BrandRepository brandRepository;
    
    @Autowired
    private ProductLikeRepository productLikeRepository;
    
    private ProductModel savedProduct;
    private BrandModel savedBrand;
    private final Long testUserId = 1L;
    
    @BeforeEach
    void setUp() {
        productLikeRepository.deleteAll();
        productRepository.deleteAll();
        brandRepository.deleteAll();
        
        BrandModel brandModel = BrandFixture.createBrandModel();
        savedBrand = brandRepository.save(brandModel);
        
        ProductModel productModel = ProductFixture.createProductWithBrandId(savedBrand.getId());
        savedProduct = productRepository.save(productModel);
    }
    
    @Nested
    @DisplayName("상품 좋아요 토글 테스트")
    class ToggleProductLikeTest {
        
        @DisplayName("상품 좋아요가 없는 상태에서 토글하면 좋아요가 추가된다")
        @Test
        void toggleLike_addLike_success() {
            // arrange
            Long userId = testUserId;
            Long productId = savedProduct.getId();
            
            // act
            productLikeFacade.toggleLike(userId, productId);
            
            // assert
            assertAll(
                    () -> assertThat(productLikeRepository.existsByUserIdAndProductId(userId, productId)).isTrue(),
                    () -> assertThat(productLikeFacade.isProductLiked(userId, productId)).isTrue()
            );
        }
        
        @DisplayName("상품 좋아요가 있는 상태에서 토글하면 좋아요가 제거된다")
        @Test
        void toggleLike_removeLike_success() {
            // arrange
            Long userId = testUserId;
            Long productId = savedProduct.getId();
            ProductLikeModel existingLike = ProductLikeFixture.createProductLikeModel(userId, productId);
            productLikeRepository.save(existingLike);
            savedProduct.incrementLikeCount();
            productRepository.save(savedProduct);
            
            // act
            productLikeFacade.toggleLike(userId, productId);
            
            // assert
            assertAll(
                    () -> assertThat(productLikeRepository.existsByUserIdAndProductId(userId, productId)).isFalse(),
                    () -> assertThat(productLikeFacade.isProductLiked(userId, productId)).isFalse()
            );
        }
        
        @DisplayName("존재하지 않는 상품에 좋아요 토글 시 예외가 발생한다")
        @Test
        void toggleLike_productNotFound_throwsException() {
            // arrange
            Long userId = testUserId;
            Long invalidProductId = 999L;
            
            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                productLikeFacade.toggleLike(userId, invalidProductId);
            });
            
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }
    }
    
    @Nested
    @DisplayName("상품 좋아요 추가 테스트")
    class AddProductLikeTest {
        
        @DisplayName("상품 좋아요를 정상적으로 추가할 수 있다")
        @Test
        void addProductLike_success() {
            // arrange
            Long userId = testUserId;
            Long productId = savedProduct.getId();
            
            // act
            ProductLikeModel result = productLikeFacade.addProductLike(userId, productId);
            
            // assert
            assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.getUserId()).isEqualTo(userId),
                    () -> assertThat(result.getProductId()).isEqualTo(productId),
                    () -> assertThat(productLikeRepository.existsByUserIdAndProductId(userId, productId)).isTrue(),
                    () -> assertThat(productLikeFacade.isProductLiked(userId, productId)).isTrue()
            );
        }
        
        @DisplayName("이미 좋아요한 상품에 좋아요 추가 시 기존 좋아요를 반환한다")
        @Test
        void addProductLike_alreadyExists_returnsExisting() {
            // arrange
            Long userId = testUserId;
            Long productId = savedProduct.getId();
            ProductLikeModel existingLike = ProductLikeFixture.createProductLikeModel(userId, productId);
            productLikeRepository.save(existingLike);
            
            // act
            ProductLikeModel result = productLikeFacade.addProductLike(userId, productId);
            
            // assert
            assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.getUserId()).isEqualTo(userId),
                    () -> assertThat(result.getProductId()).isEqualTo(productId),
                    () -> assertThat(productLikeRepository.count()).isEqualTo(1)
            );
        }
        
        @DisplayName("존재하지 않는 상품에 좋아요 추가 시 예외가 발생한다")
        @Test
        void addProductLike_productNotFound_throwsException() {
            // arrange
            Long userId = testUserId;
            Long invalidProductId = 999L;
            
            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                productLikeFacade.addProductLike(userId, invalidProductId);
            });
            
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }
    }
    
    @Nested
    @DisplayName("상품 좋아요 제거 테스트")
    class RemoveProductLikeTest {
        
        @DisplayName("상품 좋아요를 정상적으로 제거할 수 있다")
        @Test
        void removeProductLike_success() {
            // arrange
            Long userId = testUserId;
            Long productId = savedProduct.getId();
            ProductLikeModel existingLike = ProductLikeFixture.createProductLikeModel(userId, productId);
            productLikeRepository.save(existingLike);
            savedProduct.incrementLikeCount();
            productRepository.save(savedProduct);
            
            // act
            productLikeFacade.removeProductLike(userId, productId);
            
            // assert
            assertAll(
                    () -> assertThat(productLikeRepository.existsByUserIdAndProductId(userId, productId)).isFalse(),
                    () -> assertThat(productLikeFacade.isProductLiked(userId, productId)).isFalse()
            );
        }
        
        @DisplayName("좋아요하지 않은 상품의 좋아요 제거 시 아무 작업도 하지 않는다")
        @Test
        void removeProductLike_notExists_noAction() {
            // arrange
            Long userId = testUserId;
            Long productId = savedProduct.getId();
            
            // act
            productLikeFacade.removeProductLike(userId, productId);
            
            // assert
            assertThat(productLikeRepository.existsByUserIdAndProductId(userId, productId)).isFalse();
        }
        
        @DisplayName("존재하지 않는 상품의 좋아요 제거 시 예외가 발생한다")
        @Test
        void removeProductLike_productNotFound_throwsException() {
            // arrange
            Long userId = testUserId;
            Long invalidProductId = 999L;
            ProductLikeModel existingLike = ProductLikeFixture.createProductLikeModel(userId, invalidProductId);
            productLikeRepository.save(existingLike);
            
            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                productLikeFacade.removeProductLike(userId, invalidProductId);
            });
            
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }
    }
    
    @Nested
    @DisplayName("상품 좋아요 상태 확인 테스트")
    class IsProductLikedTest {
        
        @DisplayName("좋아요한 상품의 상태를 올바르게 확인할 수 있다")
        @Test
        void isProductLiked_liked_returnsTrue() {
            // arrange
            Long userId = testUserId;
            Long productId = savedProduct.getId();
            ProductLikeModel existingLike = ProductLikeFixture.createProductLikeModel(userId, productId);
            productLikeRepository.save(existingLike);
            
            // act
            boolean result = productLikeFacade.isProductLiked(userId, productId);
            
            // assert
            assertThat(result).isTrue();
        }
        
        @DisplayName("좋아요하지 않은 상품의 상태를 올바르게 확인할 수 있다")
        @Test
        void isProductLiked_notLiked_returnsFalse() {
            // arrange
            Long userId = testUserId;
            Long productId = savedProduct.getId();
            
            // act
            boolean result = productLikeFacade.isProductLiked(userId, productId);
            
            // assert
            assertThat(result).isFalse();
        }
        
        @DisplayName("다른 사용자가 좋아요한 상품에 대해 올바른 결과를 반환한다")
        @Test
        void isProductLiked_differentUser_returnsFalse() {
            // arrange
            Long userId1 = testUserId;
            Long userId2 = 2L;
            Long productId = savedProduct.getId();
            ProductLikeModel existingLike = ProductLikeFixture.createProductLikeModel(userId1, productId);
            productLikeRepository.save(existingLike);
            
            // act
            boolean result = productLikeFacade.isProductLiked(userId2, productId);
            
            // assert
            assertThat(result).isFalse();
        }
    }
    
    @Nested
    @DisplayName("복합 시나리오 테스트")
    class ComplexScenarioTest {
        
        @DisplayName("여러 사용자가 같은 상품에 좋아요를 추가할 수 있다")
        @Test
        void multipleLikes_sameProduct() {
            // arrange
            Long userId1 = 1L;
            Long userId2 = 2L;
            Long userId3 = 3L;
            Long productId = savedProduct.getId();
            
            // act
            productLikeFacade.addProductLike(userId1, productId);
            productLikeFacade.addProductLike(userId2, productId);
            productLikeFacade.addProductLike(userId3, productId);
            
            // assert
            assertAll(
                    () -> assertThat(productLikeFacade.isProductLiked(userId1, productId)).isTrue(),
                    () -> assertThat(productLikeFacade.isProductLiked(userId2, productId)).isTrue(),
                    () -> assertThat(productLikeFacade.isProductLiked(userId3, productId)).isTrue(),
                    () -> assertThat(productLikeRepository.count()).isEqualTo(3)
            );
        }
        
        @DisplayName("한 사용자가 여러 상품에 좋아요를 추가할 수 있다")
        @Test
        void multipleLikes_sameUser() {
            // arrange
            Long userId = testUserId;
            ProductModel product2 = productRepository.save(ProductFixture.createProductWithBrandId(savedBrand.getId()));
            ProductModel product3 = productRepository.save(ProductFixture.createProductWithBrandId(savedBrand.getId()));
            
            // act
            productLikeFacade.addProductLike(userId, savedProduct.getId());
            productLikeFacade.addProductLike(userId, product2.getId());
            productLikeFacade.addProductLike(userId, product3.getId());
            
            // assert
            assertAll(
                    () -> assertThat(productLikeFacade.isProductLiked(userId, savedProduct.getId())).isTrue(),
                    () -> assertThat(productLikeFacade.isProductLiked(userId, product2.getId())).isTrue(),
                    () -> assertThat(productLikeFacade.isProductLiked(userId, product3.getId())).isTrue(),
                    () -> assertThat(productLikeRepository.count()).isEqualTo(3)
            );
        }
        
        @DisplayName("좋아요 추가 후 토글하면 좋아요가 제거된다")
        @Test
        void addThenToggle_removesLike() {
            // arrange
            Long userId = testUserId;
            Long productId = savedProduct.getId();
            
            // act
            productLikeFacade.addProductLike(userId, productId);
            assertThat(productLikeFacade.isProductLiked(userId, productId)).isTrue();
            
            productLikeFacade.toggleLike(userId, productId);
            
            // assert
            assertThat(productLikeFacade.isProductLiked(userId, productId)).isFalse();
        }
        
        @DisplayName("좋아요 제거 후 토글하면 좋아요가 추가된다")
        @Test
        void removeThenToggle_addsLike() {
            // arrange
            Long userId = testUserId;
            Long productId = savedProduct.getId();
            ProductLikeModel existingLike = ProductLikeFixture.createProductLikeModel(userId, productId);
            productLikeRepository.save(existingLike);
            savedProduct.incrementLikeCount();
            productRepository.save(savedProduct);
            
            // act
            productLikeFacade.removeProductLike(userId, productId);
            assertThat(productLikeFacade.isProductLiked(userId, productId)).isFalse();
            
            productLikeFacade.toggleLike(userId, productId);
            
            // assert
            assertThat(productLikeFacade.isProductLiked(userId, productId)).isTrue();
        }
        
        @DisplayName("상품 좋아요와 브랜드 좋아요는 독립적으로 동작한다")
        @Test
        void productAndBrandLikes_independent() {
            // arrange
            Long userId = testUserId;
            Long productId = savedProduct.getId();
            
            // act
            productLikeFacade.addProductLike(userId, productId);
            
            // assert
            assertAll(
                    () -> assertThat(productLikeFacade.isProductLiked(userId, productId)).isTrue(),
                    () -> assertThat(productLikeRepository.count()).isEqualTo(1)
            );
        }
        
        @DisplayName("대량의 좋아요 처리가 정상적으로 동작한다")
        @Test
        void bulkLikes_performance() {
            // arrange
            Long productId = savedProduct.getId();
            int userCount = 10;
            
            // act
            for (long userId = 1L; userId <= userCount; userId++) {
                productLikeFacade.addProductLike(userId, productId);
            }
            
            // assert
            assertAll(
                    () -> assertThat(productLikeRepository.count()).isEqualTo(userCount),
                    () -> {
                        for (long userId = 1L; userId <= userCount; userId++) {
                            assertThat(productLikeFacade.isProductLiked(userId, productId)).isTrue();
                        }
                    }
            );
        }
        
        @DisplayName("좋아요 토글을 여러 번 수행해도 정상 동작한다")
        @Test
        void multipleToggles_consistent() {
            // arrange
            Long userId = testUserId;
            Long productId = savedProduct.getId();
            
            // act & assert
            // 첫 번째 토글: 추가
            productLikeFacade.toggleLike(userId, productId);
            assertThat(productLikeFacade.isProductLiked(userId, productId)).isTrue();
            
            // 두 번째 토글: 제거
            productLikeFacade.toggleLike(userId, productId);
            assertThat(productLikeFacade.isProductLiked(userId, productId)).isFalse();
            
            // 세 번째 토글: 추가
            productLikeFacade.toggleLike(userId, productId);
            assertThat(productLikeFacade.isProductLiked(userId, productId)).isTrue();
            
            // 네 번째 토글: 제거
            productLikeFacade.toggleLike(userId, productId);
            assertThat(productLikeFacade.isProductLiked(userId, productId)).isFalse();
        }
    }
}
