package com.loopers.domain.like;

import com.loopers.domain.like.entity.Like;
import com.loopers.domain.like.entity.LikeTargetType;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Like 테스트")
public class LikeTest {

    @DisplayName("Like 엔티티 생성")
    @Nested
    class CreateLikeTest {

        final Long validLikeTargetId = 1L;
        final LikeTargetType validLikeTargetType = LikeTargetType.PRODUCT;

        // 테스트 헬퍼 메서드
        private User createUserWithId(Long id) {
            User user = User.builder()
                    .loginId("validId123") // 실제 유효한 값 필요
                    .email("test@test.com")
                    .birthday("1990-01-01")
                    .gender(Gender.MALE)
                    .build(); // 여기서 User의 guard() 로직도 함께 검증됨!
            
            // ID 강제 주입 (ReflectionTestUtils 등 사용)
            ReflectionTestUtils.setField(user, "id", id);
            return user;
        }

        @DisplayName("성공 케이스: 필드가 모두 유효하면 Like 객체 생성 성공")
        @Test
        void createLike_withValidFields_Success() {
            // arrange
            User user = createUserWithId(1L);

            // act
            Like like = Like.builder()
                    .userId(user.getId())
                    .likeTargetId(validLikeTargetId)
                    .likeTargetType(validLikeTargetType)
                    .build();

            // assert
            assertNotNull(like);
            assertAll(
                    () -> assertNotNull(like.getLikeId().getUserId()),
                    () -> assertEquals(user.getId(), like.getLikeId().getUserId()),
                    () -> assertEquals(validLikeTargetId, like.getLikeId().getLikeTargetId()),
                    () -> assertEquals(validLikeTargetType, like.getLikeId().getLikeTargetType())
            );
        }

        @DisplayName("실패 케이스: user가 null이면 BAD_REQUEST 예외 발생")
        @Test
        void createLike_withNullUser_BadRequest() {
            // act & assert
            CoreException result = assertThrows(CoreException.class, () -> {
                Like.builder()
                        .userId(null)
                        .likeTargetId(validLikeTargetId)
                        .likeTargetType(validLikeTargetType)
                        .build();
            });

            assertEquals(ErrorType.BAD_REQUEST, result.getErrorType());
            assertEquals("Like : userId가 비어있을 수 없습니다.", result.getCustomMessage());
        }

        @DisplayName("실패 케이스: likeTargetId가 null이면 BAD_REQUEST 예외 발생")
        @Test
        void createLike_withNullLikeTargetId_BadRequest() {
            // arrange
            User user = createUserWithId(1L);

            // act & assert
            CoreException result = assertThrows(CoreException.class, () -> {
                Like.builder()
                        .userId(user.getId())
                        .likeTargetId(null)
                        .likeTargetType(validLikeTargetType)
                        .build();
            });

            assertEquals(ErrorType.BAD_REQUEST, result.getErrorType());
            assertEquals("Like : likeTargetId가 비어있을 수 없습니다.", result.getCustomMessage());
        }

        @DisplayName("실패 케이스: likeTargetId가 0 이하면 BAD_REQUEST 예외 발생")
        @Test
        void createLike_withInvalidLikeTargetId_BadRequest() {
            // arrange
            User user = createUserWithId(1L);

            // act & assert
            CoreException result = assertThrows(CoreException.class, () -> {
                Like.builder()
                        .userId(user.getId())
                        .likeTargetId(0L)
                        .likeTargetType(validLikeTargetType)
                        .build();
            });

            assertEquals(ErrorType.BAD_REQUEST, result.getErrorType());
            assertEquals("Like : likeTargetId는 양수여야 합니다.", result.getCustomMessage());
        }

        @DisplayName("실패 케이스: likeTargetType이 null이면 BAD_REQUEST 예외 발생")
        @Test
        void createLike_withNullLikeTargetType_BadRequest() {
            // arrange
            User user = createUserWithId(1L);

            // act & assert
            CoreException result = assertThrows(CoreException.class, () -> {
                Like.builder()
                        .userId(user.getId())
                        .likeTargetId(validLikeTargetId)
                        .likeTargetType(null)
                        .build();
            });

            assertEquals(ErrorType.BAD_REQUEST, result.getErrorType());
            assertEquals("Like : likeTargetType이 비어있을 수 없습니다.", result.getCustomMessage());
        }
    }
}

