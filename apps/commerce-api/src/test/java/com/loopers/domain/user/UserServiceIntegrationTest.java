package com.loopers.domain.user;

import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class UserServiceIntegrationTest {
    @Autowired
    private UserService userService;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("회원 가입할 때,")
    @Nested
    class CreateUser {
        
        @DisplayName("유효한 정보로 가입하면, 사용자가 저장된다.")
        @Test
        void savesUser_whenValidInformationIsProvided() {
            // arrange
            String id = "user123";
            String email = "test@example.com";
            String birthDate = "1990-01-01";
            String gender = "M";

            // act
            UserModel result = userService.createUser(id, email, birthDate, gender);

            // assert
            assertAll(
                () -> assertThat(result).isNotNull(),
                () -> assertThat(result.getUserId()).isEqualTo(id),
                () -> assertThat(result.getEmail()).isEqualTo(email),
                () -> assertThat(result.getBirthDate().toString()).isEqualTo(birthDate),
                () -> assertThat(result.getGender()).isEqualTo(gender),
                () -> assertThat(result.getPoint()).isEqualTo(0)
            );
            
            // 데이터베이스에 실제로 저장되었는지 확인
            UserModel savedUser = userJpaRepository.findByUserId(id);
            assertThat(savedUser).isNotNull();
            assertThat(savedUser.getUserId()).isEqualTo(id);
        }

        @DisplayName("이미 가입된 ID로 가입하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequestException_whenDuplicateIdIsProvided() {
            // arrange
            String id = "user123";
            String email1 = "test1@example.com";
            String email2 = "test2@example.com";
            String birthDate = "1990-01-01";
            String gender = "M";

            // 첫 번째 사용자 가입
            userService.createUser(id, email1, birthDate, gender);

            // act & assert - 같은 ID로 두 번째 가입 시도
            CoreException result = assertThrows(CoreException.class, () -> {
                userService.createUser(id, email2, birthDate, gender);
            });

            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(result.getMessage()).contains("이미 가입된 ID입니다");
        }
    }

    @DisplayName("사용자 조회할 때,")
    @Nested
    class GetUser {
        
        @DisplayName("존재하는 사용자 ID를 주면, 해당 사용자 정보를 반환한다.")
        @Test
        void returnsUserInfo_whenValidIdIsProvided() {
            // arrange
            String id = "user123";
            String email = "test@example.com";
            String birthDate = "1990-01-01";
            String gender = "M";
            
            UserModel savedUser = userJpaRepository.save(
                new UserModel(id, email, birthDate, gender)
            );

            // act
            UserModel result = userService.getUserById(id);

            // assert
            assertAll(
                () -> assertThat(result).isNotNull(),
                () -> assertThat(result.getUserId()).isEqualTo(id),
                () -> assertThat(result.getEmail()).isEqualTo(email),
                () -> assertThat(result.getBirthDate().toString()).isEqualTo(birthDate),
                () -> assertThat(result.getGender()).isEqualTo(gender)
            );
        }

        @DisplayName("존재하지 않는 사용자 ID를 주면, null을 반환한다.")
        @Test
        void returnsNull_whenInvalidIdIsProvided() {
            // arrange
            String invalidId = "nonexistent";

            // act
            UserModel result = userService.getUserById(invalidId);

            // assert
            assertThat(result).isNull();
        }
    }

    @DisplayName("포인트 조회할 때,")
    @Nested
    class GetUserPoint {
        
        @DisplayName("존재하는 사용자 ID를 주면, 보유 포인트를 반환한다.")
        @Test
        void returnsUserPoint_whenValidIdIsProvided() {
            // arrange
            String id = "user123";
            String email = "test@example.com";
            String birthDate = "1990-01-01";
            String gender = "M";
            
            UserModel savedUser = userJpaRepository.save(
                new UserModel(id, email, birthDate, gender)
            );

            // act
            Integer result = userService.getUserPoint(id);

            // assert
            assertThat(result).isEqualTo(0);
        }

        @DisplayName("존재하지 않는 사용자 ID를 주면, null을 반환한다.")
        @Test
        void returnsNull_whenInvalidIdIsProvided() {
            // arrange
            String invalidId = "nonexistent";

            // act
            Integer result = userService.getUserPoint(invalidId);

            // assert
            assertThat(result).isNull();
        }
    }

    @DisplayName("포인트 충전할 때,")
    @Nested
    class ChargePoint {
        
        @DisplayName("존재하지 않는 사용자 ID로 충전하면, NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsNotFoundException_whenInvalidUserIdIsProvided() {
            // arrange
            String invalidId = "nonexistent";
            Integer amount = 1000;

            // act & assert
            CoreException result = assertThrows(CoreException.class, () -> {
                userService.chargePoint(invalidId, amount);
            });

            assertThat(result.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
            assertThat(result.getMessage()).contains("사용자를 찾을 수 없습니다");
        }

        @DisplayName("존재하는 사용자에게 포인트를 충전하면, 포인트가 증가한다.")
        @Test
        void increasesPoint_whenValidUserChargesPoint() {
            // arrange
            String id = "user123";
            String email = "test@example.com";
            String birthDate = "1990-01-01";
            String gender = "M";
            Integer chargeAmount = 1000;
            
            UserModel savedUser = userJpaRepository.save(
                new UserModel(id, email, birthDate, gender)
            );

            // act
            UserModel result = userService.chargePoint(id, chargeAmount);

            // assert
            assertThat(result.getPoint()).isEqualTo(1000);
            
            // 데이터베이스에서도 확인
            UserModel updatedUser = userJpaRepository.findByUserId(id);
            assertThat(updatedUser.getPoint()).isEqualTo(1000);
        }
    }
}
