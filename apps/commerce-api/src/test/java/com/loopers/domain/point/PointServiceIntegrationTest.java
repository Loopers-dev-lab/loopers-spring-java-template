package com.loopers.domain.point;

import com.loopers.application.point.PointFacade;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
public class PointServiceIntegrationTest {
    @Autowired
    private PointFacade pointFacade;
    @Autowired
    private PointService pointService;
    @Autowired
    private UserService userService;

    @BeforeEach
    void setUp() {
        String validId = "user123";
        String validEmail = "xx@yy.zz";
        String validBirthday = "1993-03-13";
        String validGender = "male";
        // 유저 등록
        User registeredUser = userService.registerUser(validId, validEmail, validBirthday, validGender);
        pointService.createPoint(registeredUser.getId());
    }

    @DisplayName("해당 ID 의 회원이 존재할 경우, 보유 포인트가 반환된다.")
    @Test
    void returnsUserPoints_whenUserExists() {
        // arrange: setUp() 메서드에서 이미 유저 등록
        String existingUserId = "user123";
        Long userId = userService.findByUserId(existingUserId).get().getId();

        // act
        Optional<Long> currentPoint = pointService.getCurrentPoint(userId);

        // assert
        assertThat(currentPoint.orElse(null)).isEqualTo(0L);
    }

    @DisplayName("해당 ID 의 회원이 존재하지 않을 경우, null 이 반환된다.")
    @Test
    void returnsNullPoints_whenUserDoesNotExist() {
        // arrange: setUp() 메서드에서 등록되지 않은 유저 ID 사용
        Long nonExistingUserId = -1L;

        // act
        Optional<Long> currentPoint = pointService.getCurrentPoint(nonExistingUserId);

        // assert
        assertThat(currentPoint).isNotPresent();
    }

    //존재하지 않는 유저 ID 로 충전을 시도한 경우, 실패한다.
    @DisplayName("존재하지 않는 유저 ID 로 충전을 시도한 경우, 실패한다.")
    @Test
    void throwsExceptionWhenChargePointWithNonExistingUserId() {
        // arrange
        String nonExistingUserId = "nonexist";
        int chargeAmount = 1000;

        // act & assert
        assertThrows(CoreException.class, () -> pointFacade.chargePoint(nonExistingUserId, chargeAmount));
    }
}
