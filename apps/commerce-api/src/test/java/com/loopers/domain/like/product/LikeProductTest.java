package com.loopers.domain.like.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("상품 좋아요(LikeProduct) Entity 테스트")
public class LikeProductTest {

    @DisplayName("좋아요를 생성할 때, ")
    @Nested
    class Create {
        @DisplayName("정상적인 userId와 productId로 좋아요를 생성할 수 있다. (Happy Path)")
        @Test
        void should_createLikeProduct_when_validUserIdAndProductId() {
            // arrange
            Long userId = 1L;
            Long productId = 100L;

            // act
            LikeProduct likeProduct = LikeProduct.create(userId, productId);

            // assert
            assertThat(likeProduct.getUserId()).isEqualTo(1L);
            assertThat(likeProduct.getProductId()).isEqualTo(100L);
        }

        @DisplayName("userId가 0인 경우, 예외가 발생한다. (Exception)")
        @Test
        void should_throwException_when_userIdIsZero() {
            // arrange
            Long userId = 0L;
            Long productId = 100L;

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> LikeProduct.create(userId, productId));
            assertThat(exception.getMessage()).isEqualTo("사용자 ID는 1 이상이어야 합니다.");
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("productId가 0인 경우, 예외가 발생한다. (Exception)")
        @Test
        void should_throwException_when_productIdIsZero() {
            // arrange
            Long userId = 1L;
            Long productId = 0L;

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> LikeProduct.create(userId, productId));
            assertThat(exception.getMessage()).isEqualTo("상품 ID는 1 이상이어야 합니다.");
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("userId와 productId가 모두 0인 경우, 예외가 발생한다. (Exception)")
        @Test
        void should_throwException_when_bothIdsAreZero() {
            // arrange
            Long userId = 0L;
            Long productId = 0L;

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> LikeProduct.create(userId, productId));
            assertThat(exception.getMessage()).isEqualTo("사용자 ID는 1 이상이어야 합니다.");
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("음수 userId인 경우, 예외가 발생한다. (Exception)")
        @Test
        void should_throwException_when_userIdIsNegative() {
            // arrange
            Long userId = -1L;
            Long productId = 100L;

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> LikeProduct.create(userId, productId));
            assertThat(exception.getMessage()).isEqualTo("사용자 ID는 1 이상이어야 합니다.");
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("음수 productId인 경우, 예외가 발생한다. (Exception)")
        @Test
        void should_throwException_when_productIdIsNegative() {
            // arrange
            Long userId = 1L;
            Long productId = -1L;

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> LikeProduct.create(userId, productId));
            assertThat(exception.getMessage()).isEqualTo("상품 ID는 1 이상이어야 합니다.");
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("null userId인 경우, 예외가 발생한다. (Exception)")
        @Test
        void should_throwException_when_userIdIsNull() {
            // arrange
            Long userId = null;
            Long productId = 100L;

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> LikeProduct.create(userId, productId));
            assertThat(exception.getMessage()).isEqualTo("사용자 ID는 1 이상이어야 합니다.");
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("null productId인 경우, 예외가 발생한다. (Exception)")
        @Test
        void should_throwException_when_productIdIsNull() {
            // arrange
            Long userId = 1L;
            Long productId = null;

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> LikeProduct.create(userId, productId));
            assertThat(exception.getMessage()).isEqualTo("상품 ID는 1 이상이어야 합니다.");
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @DisplayName("좋아요 조회를 할 때, ")
    @Nested
    class Retrieve {
        @DisplayName("생성한 좋아요의 userId를 조회할 수 있다. (Happy Path)")
        @Test
        void should_retrieveUserId_when_likeProductCreated() {
            // arrange
            Long userId = 1L;
            Long productId = 100L;
            LikeProduct likeProduct = LikeProduct.create(userId, productId);

            // act
            Long retrievedUserId = likeProduct.getUserId();

            // assert
            assertThat(retrievedUserId).isEqualTo(1L);
        }

        @DisplayName("생성한 좋아요의 productId를 조회할 수 있다. (Happy Path)")
        @Test
        void should_retrieveProductId_when_likeProductCreated() {
            // arrange
            Long userId = 1L;
            Long productId = 100L;
            LikeProduct likeProduct = LikeProduct.create(userId, productId);

            // act
            Long retrievedProductId = likeProduct.getProductId();

            // assert
            assertThat(retrievedProductId).isEqualTo(100L);
        }
    }

    @DisplayName("좋아요 동등성을 확인할 때, ")
    @Nested
    class Equality {
        @DisplayName("같은 userId와 productId를 가진 좋아요는 서로 다른 인스턴스이다. (Edge Case)")
        @Test
        void should_beDifferentInstances_when_sameUserIdAndProductId() {
            // arrange
            Long userId = 1L;
            Long productId = 100L;
            LikeProduct likeProduct1 = LikeProduct.create(userId, productId);
            LikeProduct likeProduct2 = LikeProduct.create(userId, productId);

            // act & assert
            assertThat(likeProduct1).isNotSameAs(likeProduct2);
            assertThat(likeProduct1).isNotEqualTo(likeProduct2);
        }

        @DisplayName("다른 userId를 가진 좋아요는 서로 다른 인스턴스이다. (Happy Path)")
        @Test
        void should_beDifferentInstances_when_differentUserId() {
            // arrange
            LikeProduct likeProduct1 = LikeProduct.create(1L, 100L);
            LikeProduct likeProduct2 = LikeProduct.create(2L, 100L);

            // act & assert
            assertThat(likeProduct1).isNotSameAs(likeProduct2);
            assertThat(likeProduct1).isNotEqualTo(likeProduct2);
            assertThat(likeProduct1.getUserId()).isNotEqualTo(likeProduct2.getUserId());
        }

        @DisplayName("다른 productId를 가진 좋아요는 서로 다른 인스턴스이다. (Happy Path)")
        @Test
        void should_beDifferentInstances_when_differentProductId() {
            // arrange
            LikeProduct likeProduct1 = LikeProduct.create(1L, 100L);
            LikeProduct likeProduct2 = LikeProduct.create(1L, 200L);

            // act & assert
            assertThat(likeProduct1).isNotSameAs(likeProduct2);
            assertThat(likeProduct1).isNotEqualTo(likeProduct2);
            assertThat(likeProduct1.getProductId()).isNotEqualTo(likeProduct2.getProductId());
        }
    }
}
