package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = com.loopers.CommerceCoreApplication.class)
@Transactional
public class UserServiceIntegrationTest {
    @Autowired
    private UserService userService;

    @MockitoSpyBean
    private UserRepository spyUserRepository;

    @DisplayName("회원 가입시 User 저장이 수행된다.")
    @Test
    void saveUserWhenRegister() {
        // arrange
        String validId = "user123";
        String validEmail = "xx@yy.zz";
        String validBirthday = "1993-03-13";
        String validGender = "male";

        // act
        // 유저 등록
        userService.registerUser(validId, validEmail, validBirthday, validGender);
        // 저장된 유저 조회
        Optional<User> foundUser = userService.findByUserId(validId);

        // assert
        verify(spyUserRepository).save(any(User.class));
        verify(spyUserRepository).findByUserId("user123");
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUserId()).isEqualTo("user123");
    }

    @DisplayName("이미 가입된 ID 로 회원가입 시도 시, 실패한다.")
    @Test
    void throwsExceptionWhenRegisterWithExistingUserId() {
        // arrange
        String validId = "user123";
        String validEmail = "xx@yy.zz";
        String validBirthday = "1993-03-13";
        String validGender = "male";

        // act
        // 기존 유저 등록
        userService.registerUser(validId, validEmail, validBirthday, validGender);
        // 동일 ID 로 유저 등록 시도
        CoreException result = assertThrows(CoreException.class, () -> {
            userService.registerUser(validId, "zz@cc.xx", "1992-06-07", "female");
        });

        // assert
        assertThat(result.getMessage()).isEqualTo("이미 존재하는 사용자 ID 입니다.");
    }

    @DisplayName("해당 ID 의 회원이 존재할 경우, 회원 정보가 반환된다.")
    @Test
    void returnsUserInfo_whenUserExists() {
        // arrange
        String validId = "user123";
        String validEmail = "xx@yy.zz";
        String validBirthday = "1993-03-13";
        String validGender = "male";
        // 기존 유저 등록
        userService.registerUser(validId, validEmail, validBirthday, validGender);

        // act
        Optional<User> foundUser = userService.findByUserId(validId);

        // assert
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getUserId()).isEqualTo("user123");
    }

    @DisplayName("해당 ID 의 회원이 존재하지 않을 경우, null 이 반환된다.")
    @Test
    void returnsNull_whenUserDoesNotExist() {
        // arrange
        String validId = "user123";
        String validEmail = "xx@yy.zz";
        String validBirthday = "1993-03-13";
        String validGender = "male";
        // 기존 유저 등록
        userService.registerUser(validId, validEmail, validBirthday, validGender);
        String nonExistId = "nonexist";


        // act
        Optional<User> foundUser = userService.findByUserId(nonExistId);

        // assert
        assertThat(foundUser).isNotPresent();
    }
}
