package com.loopers.domain.user;

import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.interfaces.api.user.UserDtoMapper;
import com.loopers.interfaces.api.user.UserRequestDto;
import com.loopers.support.error.CoreException;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserIntegrationTest {

    @Autowired
    private UserService userService;

    @MockitoSpyBean
    private UserJpaRepository userJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }
    @Test
    @DisplayName("회원 가입 성공 시 User 저장이 수행된다 (spy 검증)")
    void signUp_success_saves_user() {
        //given
        UserRequestDto requestDto = new UserRequestDto(
                "sangdon",
                "dori@dori.com",
                "1998-02-21",
                "MALE"
        );
        User user = new UserDtoMapper().toEntity(requestDto);

        //when
        userService.saveUser(user);

        //then
        verify(userJpaRepository, times(1)).save(user);
        
    }

    @Test
    @DisplayName("이미 가입된 ID로 회원가입 시 실패한다.")
    void signUp_duplicate_id_fails() {
        //given
        UserRequestDto requestDto1 = new UserRequestDto(
                "sangdon",
                "dori@dori.com",
                "1998-02-21",
                "MALE"
        );
        User user1 = new UserDtoMapper().toEntity(requestDto1);

        UserRequestDto requestDto2 = new UserRequestDto(
                "sangdon",
                "karina@karina.com",
                "2000-02-21",
                "FEMALE"
        );
        User user2 = new UserDtoMapper().toEntity(requestDto2);

        //when
        userService.saveUser(user1);

        //then
        assertThrows(CoreException.class, () -> userService.saveUser(user2));
        verify(userJpaRepository, times(1)).save(user1);
        verify(userJpaRepository, never()).save(user2);
    }

    @Test
    @DisplayName("해당 ID 의 회원이 존재할 경우, 회원 정보가 반환된다.")
    void returns_user_info_when_id_exist(){

        //given
        User user = User.builder()
                .id("dori")
                .email("dori@dori.com")
                .birthDate("1998-02-21")
                .gender("M")
                .build();
        
        userJpaRepository.save(user);
        
        //when
        User result = userService.getUser(user.getId());
        //then
        assertAll(
            () -> assertThat(result.getId()).isEqualTo(user.getId()),
            () -> assertThat(result.getEmail()).isEqualTo(user.getEmail()),
            () -> assertThat(result.getBirthDate()).isEqualTo(user.getBirthDate()),
            () -> assertThat(result.getGender()).isEqualTo(user.getGender())
        );
    }

    @Test
    @DisplayName("해당 ID 의 회원이 존재하지 않을 경우, 예외가 발생한다.")
    void throws_when_id_not_exist(){
        //given
        String id = "dori";

        // when
        User result = userService.getUser(id);
        //then
        assertThat(result).isNull();
    }
}
