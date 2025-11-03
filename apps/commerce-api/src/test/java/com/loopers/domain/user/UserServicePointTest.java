package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

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

    @DisplayName("존재하지 않는 유저 ID 로 충전을 시도한 경우, 실패한다.")
    @Test
    void throwBadRequestException_whenChargePointUserDoesNotExist() {
        // given
        String userId = "unknown";
        int point = 500;
        given(userRepository.findByUserId(userId))
                .willReturn(Optional.empty());

        // when
        CoreException ex = assertThrows(CoreException.class,
                () -> userService.chargePoint(userId, point));

        // then
        assertThat(ex.getErrorType()).isEqualTo(ErrorType.NOT_FOUND); // 상태
        verify(userRepository).findByUserId(userId); // 행위
        verify(userRepository, never()).save(any()); //  행위
    }

    @DisplayName("유효한 이용자 ID에 대한 충전 시도시, 성공한다")
    @Test
    void doChargePoint_whenUserExists() {
        // given
        String userId = userModel.getUserId();
        int originalPoint = userModel.getPoint();
        int newPoint = 500;
        given(userRepository.findByUserId(userId))
                .willReturn(Optional.of(userModel));

        // when
        ArgumentCaptor<UserModel> userCaptor = ArgumentCaptor.forClass(UserModel.class);
        Integer afterPoint = userService.chargePoint(userId, newPoint);

        // then
        assertThat(afterPoint).isEqualTo(originalPoint + newPoint); // 상태

        verify(userRepository, atLeastOnce()).findByUserId(userId); // 행위
        verify(userRepository, times(1)).save(userCaptor.capture()); // save에 전달된 객체 캡처

        // 캡처된 객체가 올바르게 업데이트되었는지 검증
        assertThat(userCaptor.getValue().getPoint()).isEqualTo(originalPoint + newPoint);
    }


}
