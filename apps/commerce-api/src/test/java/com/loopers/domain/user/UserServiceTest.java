package com.loopers.domain.user;

import com.loopers.application.user.UserFacade;
import com.loopers.interfaces.api.user.UserV1Dto;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
public class UserServiceTest {
    @Autowired
    private UserFacade userFacade;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @DisplayName(" 회원 가입시,")
    @Nested
    class get {
        @DisplayName("이미 가입된 LoginId가 있는 경우 실패 한다.")
        @Test
        void throwsException_whenIdAlreadyExists() {
            // arrange
          UserEntity userEntity = UserFixture.createUser();

            UserV1Dto.CreateUserRequest request = new UserV1Dto.CreateUserRequest(
                    UserFixture.USER_LOGIN_ID,
                    UserFixture.USER_EMAIL,
                    UserFixture.USER_BIRTH_DATE,
                    UserFixture.USER_GENDER
            );

            userRepository.save(userEntity);

            // act
            CoreException exception = assertThrows(
                CoreException.class, () -> userFacade.createUser(request));

            //assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }



}
