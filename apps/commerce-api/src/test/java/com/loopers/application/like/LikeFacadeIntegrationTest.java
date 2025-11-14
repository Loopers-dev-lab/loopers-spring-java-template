package com.loopers.application.like;

import com.loopers.application.product.ProductFacade;
import com.loopers.application.product.ProductInfo;
import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.domain.like.Like;
import com.loopers.domain.like.LikeTargetType;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductStatus;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.infrastructure.like.LikeJpaRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("LikeFacade 통합 테스트")
@SpringBootTest
class LikeFacadeIntegrationTest {

    @Autowired
    private LikeFacade likeFacade;

    @Autowired
    private UserFacade userFacade;

    @Autowired
    private UserService userService;

    @Autowired
    private LikeJpaRepository likeJpaRepository;

    @Autowired
    private ProductFacade productFacade;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    final String validLoginId = "bobby34";
    final String validEmail = "bobby34@naver.com";
    final String validBirthday = "1994-04-08";
    final Gender validGender = Gender.MALE;

    @DisplayName("saveProductLike 메서드")
    @Nested
    class saveProductLikeTest {

        @DisplayName("성공 케이스: 유효한 사용자와 상품 ID로 좋아요 등록 시 성공")
        @Test
        void saveProductLike_withValidUserAndProduct_Success() {
            // arrange
            createAndSaveUser(validLoginId);
            User user = userService.findUserByLoginId(validLoginId).orElseThrow();
            Long userId = user.getId();
            ProductInfo productInfo = createAndSaveProduct("테스트 상품", BigDecimal.valueOf(10000L), 0L);
            Long productId = productInfo.id();

            // act
            likeFacade.saveProductLike(validLoginId, productId);

            // assert
            Optional<Like> savedLike = likeJpaRepository.findByUser_IdAndLikeTargetIdAndLikeTargetType(
                    userId, productId, LikeTargetType.PRODUCT
            );
            assertTrue(savedLike.isPresent());
            assertEquals(userId, savedLike.get().getUser().getId());
            assertEquals(productId, savedLike.get().getLikeTargetId());
            assertEquals(LikeTargetType.PRODUCT, savedLike.get().getLikeTargetType());
            
            // Product.likeCount가 1 증가했는지 확인
            Product product = productRepository.findById(productId).orElseThrow();
            assertEquals(1L, product.getLikeCount(), "좋아요 등록 시 likeCount가 1 증가해야 합니다.");
        }

        @DisplayName("멱등성 테스트: 동일한 좋아요를 중복 등록해도 예외 없이 처리")
        @Test
        void saveProductLike_duplicateLike_Idempotent() {
            // arrange
            createAndSaveUser(validLoginId);
            User user = userService.findUserByLoginId(validLoginId).orElseThrow();
            Long userId = user.getId();
            ProductInfo productInfo = createAndSaveProduct("테스트 상품", BigDecimal.valueOf(10000L), 0L);
            Long productId = productInfo.id();

            // act
            likeFacade.saveProductLike(validLoginId, productId);
            Product productAfterFirst = productRepository.findById(productId).orElseThrow();
            Long likeCountAfterFirst = productAfterFirst.getLikeCount();
            
            likeFacade.saveProductLike(validLoginId, productId); // 중복 등록

            // assert
            Optional<Like> savedLike = likeJpaRepository.findByUser_IdAndLikeTargetIdAndLikeTargetType(
                    userId, productId, LikeTargetType.PRODUCT
            );
            assertTrue(savedLike.isPresent());
            // 멱등성: 중복 등록해도 하나만 존재
            long count = likeJpaRepository.findAll().stream()
                    .filter(like -> like.getUser().getId().equals(userId)
                            && like.getLikeTargetId().equals(productId)
                            && like.getLikeTargetType() == LikeTargetType.PRODUCT)
                    .count();
            assertThat(count).isEqualTo(1);
            
            // 멱등성: 중복 등록 시 likeCount가 증가하지 않아야 함
            Product productAfterSecond = productRepository.findById(productId).orElseThrow();
            assertEquals(likeCountAfterFirst, productAfterSecond.getLikeCount(), 
                    "중복 등록 시 likeCount가 증가하지 않아야 합니다.");
        }

        @DisplayName("실패 케이스: 존재하지 않는 사용자로 좋아요 등록 시 NOT_FOUND 예외 발생")
        @Test
        void saveProductLike_withNonExistentUser_NotFound() {
            // arrange
            String nonExistentLoginId = "nonexistent";
            Long productId = 1L;

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                likeFacade.saveProductLike(nonExistentLoginId, productId);
            });

            assertEquals(ErrorType.NOT_FOUND, exception.getErrorType());
            assertTrue(exception.getCustomMessage().contains("[loginId = " + nonExistentLoginId + "] User를 찾을 수 없습니다."));
        }
    }

    @DisplayName("deleteProductLike 메서드")
    @Nested
    class DeleteProductLikeTest {

        @DisplayName("성공 케이스: 존재하는 좋아요를 취소하면 삭제됨")
        @Test
        void deleteProductLike_withExistingLike_Success() {
            // arrange
            createAndSaveUser(validLoginId);
            User user = userService.findUserByLoginId(validLoginId).orElseThrow();
            Long userId = user.getId();
            ProductInfo productInfo = createAndSaveProduct("테스트 상품", BigDecimal.valueOf(10000L), 0L);
            Long productId = productInfo.id();
            likeFacade.saveProductLike(validLoginId, productId);
            
            Product productBeforeDelete = productRepository.findById(productId).orElseThrow();
            Long likeCountBeforeDelete = productBeforeDelete.getLikeCount();

            // act
            likeFacade.deleteProductLike(validLoginId, productId);

            // assert
            Optional<Like> deletedLike = likeJpaRepository.findByUser_IdAndLikeTargetIdAndLikeTargetType(
                    userId, productId, LikeTargetType.PRODUCT
            );
            assertFalse(deletedLike.isPresent());
            
            // Product.likeCount가 1 감소했는지 확인
            Product productAfterDelete = productRepository.findById(productId).orElseThrow();
            assertEquals(likeCountBeforeDelete - 1, productAfterDelete.getLikeCount(), 
                    "좋아요 삭제 시 likeCount가 1 감소해야 합니다.");
        }

        @DisplayName("멱등성 테스트: 존재하지 않는 좋아요를 취소해도 예외 없이 처리")
        @Test
        void deleteProductLike_withNonExistentLike_Idempotent() {
            // arrange
            createAndSaveUser(validLoginId);
            User user = userService.findUserByLoginId(validLoginId).orElseThrow();
            Long userId = user.getId();
            ProductInfo productInfo = createAndSaveProduct("테스트 상품", BigDecimal.valueOf(10000L), 5L);
            Long productId = productInfo.id();
            
            Product productBeforeDelete = productRepository.findById(productId).orElseThrow();
            Long likeCountBeforeDelete = productBeforeDelete.getLikeCount();

            // act & assert - 예외 없이 처리되어야 함
            assertDoesNotThrow(() -> {
                likeFacade.deleteProductLike(validLoginId, productId);
            });

            // assert
            Optional<Like> deletedLike = likeJpaRepository.findByUser_IdAndLikeTargetIdAndLikeTargetType(
                    userId, productId, LikeTargetType.PRODUCT
            );
            assertFalse(deletedLike.isPresent());
            
            // 멱등성: 존재하지 않는 좋아요 삭제 시 likeCount가 변하지 않아야 함
            Product productAfterDelete = productRepository.findById(productId).orElseThrow();
            assertEquals(likeCountBeforeDelete, productAfterDelete.getLikeCount(), 
                    "존재하지 않는 좋아요 삭제 시 likeCount가 변하지 않아야 합니다.");
        }

        @DisplayName("실패 케이스: 존재하지 않는 사용자로 좋아요 취소 시 NOT_FOUND 예외 발생")
        @Test
        void deleteProductLike_withNonExistentUser_NotFound() {
            // arrange
            String nonExistentLoginId = "nonexistent";
            Long productId = 1L;

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                likeFacade.deleteProductLike(nonExistentLoginId, productId);
            });

            assertEquals(ErrorType.NOT_FOUND, exception.getErrorType());
            assertTrue(exception.getCustomMessage().contains("[loginId = " + nonExistentLoginId + "] User를 찾을 수 없습니다."));
        }
    }

    // 테스트 헬퍼 메서드
    private UserInfo createAndSaveUser(String loginId) {
        UserInfo userInfo = UserInfo.builder()
                .loginId(loginId)
                .email(validEmail)
                .birthday(validBirthday)
                .gender(validGender)
                .build();
        return userFacade.saveUser(userInfo);
    }

    private ProductInfo createAndSaveProduct(String name, BigDecimal price, Long likeCount) {
        Product product = Product.builder()
                .name(name)
                .description("테스트 설명")
                .price(price)
                .likeCount(likeCount)
                .status(ProductStatus.ON_SALE)
                .isVisible(true)
                .isSellable(true)
                .build();

        return productFacade.saveProduct(ProductInfo.from(product));
    }
}

