package com.loopers.domain.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class UserServicePointTest {
    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserService userService;

    private UserModel userModel;

    @BeforeEach
    void setUp() {
        String userId = "ajchoi0928";
        String userName = "junho";
        String description = "loopers backend developer";
        String email = "loopers@loopers.com";
        String birthDate = "1997-09-28";
        String gender = "M";
        Integer points = 1000;
        userModel = new UserModel(userId, userName, description, email, birthDate, gender, points);
    }


    @DisplayName("해당 ID 의 회원이 존재할 경우, 보유 포인트가 반환된다")
    @Test
    void returnPoint_whenUserExists() {
        // given
        given(userRepository.findByUserId(userModel.getUserId()))
                .willReturn(Optional.of(userModel));

        // when
        Integer points = userService.getUserOrNull(userModel.getUserId()).getPoint();

        // then
        // 상태와 행위검증
        assertThat(points).isEqualTo(userModel.getPoint());
        verify(userRepository).findByUserId(userModel.getUserId());
    }

    @DisplayName("해당 ID 의 회원이 존재하지 않을 경우, null 이 반환된다")
    @Test
    void returnNull_whenUserDoesNotExist() {
        // given
        given(userRepository.findByUserId(userModel.getUserId()))
                .willReturn(Optional.empty());

        // when
        UserModel foundUser = userService.getUserOrNull(userModel.getUserId());

        // then
        assertThat(foundUser).isNull();
        verify(userRepository).findByUserId(userModel.getUserId());
    }
}
