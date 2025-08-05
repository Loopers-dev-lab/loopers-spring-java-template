package com.loopers.application.user;

import com.loopers.domain.user.UserModel;
import com.loopers.domain.user.UserFixture;
import com.loopers.domain.user.UserRepository;
import com.loopers.domain.user.UserService;
import com.loopers.interfaces.api.user.UserV1Dto;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@SpringBootTest
public class UserFacadeIntegrationTest {

    @Autowired
    private UserFacade userFacade;

    @MockitoSpyBean
    private UserService userService;

    @MockitoSpyBean
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("[통합 테스트]회원 가입 시")
    class CreateUserTest {
        
        @DisplayName("회원 가입시 User 저장이 수행된다.")
        @Test
        void saveUser_whenSignUpIsSuccessful() {
            // arrange
            UserModel userModel = UserFixture.createUser();
            UserV1Dto.CreateUserRequest request = new UserV1Dto.CreateUserRequest(
                    userModel.getLoginId(),
                    userModel.getEmail(),
                    userModel.getBirth(),
                    userModel.getGrender()
            );
            // act
            UserCommand.UserResponse result = userFacade.createUser(request);

            // assert
            assertThat(result).isNotNull();
            assertThat(result.loginId()).isEqualTo(userModel.getLoginId());
            assertThat(result.email()).isEqualTo(userModel.getEmail());
            assertThat(result.birth()).isEqualTo(userModel.getBirth());
            assertThat(result.gender()).isEqualTo(userModel.getGrender());

            verify(userRepository).save(any());
        }

        @DisplayName("이미 가입된 LoginID 로 회원가입 시도 시, 실패한다.")
        @Test
        void throwsException_whenLoginIdAlreadyExists() {
            // arrange
            UserModel existingUser = UserFixture.createUser();
            userRepository.save(existingUser);

            UserV1Dto.CreateUserRequest request = new UserV1Dto.CreateUserRequest(
                    existingUser.getLoginId(),
                    existingUser.getEmail(),
                    existingUser.getBirth(),
                    existingUser.getGrender()
            );

            // act
            CoreException exception = assertThrows(
                    CoreException.class,
                    () -> userFacade.createUser(request)
            );
            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("[통합 테스트]회원 조회 시")
    class GetUserTest {
        @DisplayName("해당 ID 의 회원이 존재할 경우, 회원 정보가 반환된다.")
        @Test
        void returnsUserInfo_whenUserExists() {
            // arrange
            UserModel userModel = UserFixture.createUser();
            UserModel result = userRepository.save(userModel);
            // act
            UserCommand.UserResponse userInfo = userFacade.getUserById(result.getId());
            // assert
            AssertionsForClassTypes.assertThat(userInfo).isNotNull();
            AssertionsForClassTypes.assertThat(userInfo.loginId()).isEqualTo(result.getLoginId());
            AssertionsForClassTypes.assertThat(userInfo.email()).isEqualTo(result.getEmail());
            AssertionsForClassTypes.assertThat(userInfo.birth()).isEqualTo(result.getBirth());
            AssertionsForClassTypes.assertThat(userInfo.gender()).isEqualTo(result.getGrender());
        }

        @DisplayName("해당 ID 의 회원이 존재하지 않을 경우,  null 이 반환된다.")
        @Test
        void throwsException_whenUserDoesNotExist() {
            // arrange
            Long id = 5L;
            // act
            UserCommand.UserResponse info = userFacade.getUserById(id);
            // assert
            AssertionsForClassTypes.assertThat(info).isNull();
        }
    }
}
