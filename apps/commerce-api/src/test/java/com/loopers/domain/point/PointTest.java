package com.loopers.domain.point;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Point 테스트")
public class PointTest {

    private User createValidUser() {
        return User.builder()
                .loginId("testuser1")
                .email("test@test.com")
                .birthday("1990-01-01")
                .gender(Gender.MALE)
                .build();
    }

    @DisplayName("Point 엔티티 생성")
    @Nested
    class CreatePointTest {

        @DisplayName("성공 케이스: 필드가 모두 유효하면 Point 객체 생성 성공")
        @Test
        void createPoint_withValidFields_Success() {
            // arrange
            User user = createValidUser();

            // act
            Point point = Point.builder()
                    .user(user)
                    .build();

            // assert
            assertNotNull(point);
            assertAll(
                    () -> assertEquals(BigDecimal.ZERO, point.getAmount(), "amount는 필드 초기화로 BigDecimal.ZERO가 기본값"),
                    () -> assertEquals(user, point.getUser())
            );
        }

        @DisplayName("실패 케이스: user가 null이면 예외 발생")
        @Test
        void createPoint_withNullUser_ThrowsException() {
            // act & assert
            CoreException exception = assertThrows(CoreException.class, () ->
                    Point.builder()
                            .user(null)
                            .build()
            );

            assertEquals(ErrorType.BAD_REQUEST, exception.getErrorType());
            assertTrue(exception.getCustomMessage().contains("user가 Null 이 되면 안 됩니다"));
        }

        // @DisplayName("실패 케이스: amount가 null이면 예외 발생")
        // @Test
        // void createPoint_withNullAmount_ThrowsException() {
        //     // arrange
        //     User user = createValidUser();
        //     Point point = Point.builder()
        //             .user(user)
        //             .build();

        //     // 리플렉션을 사용하여 amount를 null로 설정
        //     try {
        //         java.lang.reflect.Field amountField = Point.class.getDeclaredField("amount");
        //         amountField.setAccessible(true);
        //         amountField.set(point, null);
        //     } catch (Exception e) {
        //         throw new RuntimeException("amount 필드 설정 실패", e);
        //     }

        //     // act & assert
        //     CoreException exception = assertThrows(CoreException.class, () -> {
        //         // guard()를 리플렉션으로 호출하여 검증
        //         java.lang.reflect.Method guardMethod = point.getClass().getSuperclass().getDeclaredMethod("guard");
        //         guardMethod.setAccessible(true);
        //         guardMethod.invoke(point);
        //     });

        //     assertEquals(ErrorType.BAD_REQUEST, exception.getErrorType());
        //     assertEquals(exception.getCustomMessage(), "Point : amount가 Null 이 되면 안 됩니다.");
        // }

        // @DisplayName("실패 케이스: amount가 음수이면 예외 발생")
        // @Test
        // void createPoint_withNegativeAmount_ThrowsException() {
        //     // arrange
        //     User user = createValidUser();
        //     Point point = Point.builder()
        //             .user(user)
        //             .build();

        //     // 리플렉션을 사용하여 amount를 음수로 설정
        //     try {
        //         java.lang.reflect.Field amountField = Point.class.getDeclaredField("amount");
        //         amountField.setAccessible(true);
        //         amountField.set(point, BigDecimal.valueOf(-1000));
        //     } catch (Exception e) {
        //         throw new RuntimeException("amount 필드 설정 실패", e);
        //     }

        //     // act & assert
        //     CoreException exception = assertThrows(CoreException.class, () -> {
        //         // guard()를 리플렉션으로 호출하여 검증
        //         java.lang.reflect.Method guardMethod = point.getClass().getSuperclass().getDeclaredMethod("guard");
        //         guardMethod.setAccessible(true);
        //         guardMethod.invoke(point);
        //     });

        //     assertEquals(ErrorType.BAD_REQUEST, exception.getErrorType());
        //     assertTrue(exception.getCustomMessage().contains("amount는 음수가 될 수 없습니다"));
        // }

        @DisplayName("성공 케이스: amount가 0이면 Point 객체 생성 성공")
        @Test
        void createPoint_withZeroAmount_Success() {
            // arrange
            User user = createValidUser();

            // act
            Point point = Point.builder()
                    .user(user)
                    .build();

            // assert
            assertNotNull(point);
            assertEquals(BigDecimal.ZERO, point.getAmount());
        }
    }
}
