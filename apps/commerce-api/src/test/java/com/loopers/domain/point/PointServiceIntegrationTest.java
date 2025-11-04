package com.loopers.domain.point;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.loopers.application.point.PointCommand;
import com.loopers.application.point.PointFacade;
import com.loopers.application.point.PointInfo;
import com.loopers.application.user.UserCommand.SignupCommand;
import com.loopers.domain.user.UserService;
import java.util.Optional;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
class PointServiceIntegrationTest {

    @Autowired
    private PointFacade pointFacade;

    @MockitoSpyBean
    private UserService userService;

    @MockitoSpyBean
    private PointRepository pointRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("포인트를 충전할 때,")
    @Nested
    class ChargePoint {

        @DisplayName("존재하지 않는 유저 ID로 충전을 시도한 경우, 실패한다.")
        @Test
        void throwsException_whenUserDoesNotExist() {
            // arrange
            String nonExistentLoginId = "nonExistentUser";
            Integer chargeAmount = 1000;
            PointCommand.ChargeCommand command = new PointCommand.ChargeCommand(nonExistentLoginId, chargeAmount);

            doReturn(Optional.empty()).when(userService).findUserByLoginId(nonExistentLoginId);

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                pointFacade.chargePoint(command);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
            assertThat(exception.getMessage()).contains("사용자를 찾을 수 없습니다");

            // verify
            verify(userService, times(1)).findUserByLoginId(nonExistentLoginId);
            verify(pointRepository, never()).save(any(Point.class));
        }

        @DisplayName("존재하는 유저가 처음 포인트를 충전하면 성공한다.")
        @Test
        void chargesPoint_whenUserExistsAndNewPoint() {
            // arrange
            SignupCommand signupCommand = new SignupCommand(
                    "testId123",
                    "test@test.com",
                    "2000-03-29",
                    "M"
            );
            userService.signup(signupCommand);

            Integer chargeAmount = 1000;
            PointCommand.ChargeCommand command = new PointCommand.ChargeCommand("testId123", chargeAmount);

            // act
            PointInfo pointInfo = pointFacade.chargePoint(command);

            // assert
            assertThat(pointInfo.totalPoint()).isEqualTo(1000);

            // verify
            verify(pointRepository, times(1)).save(any(Point.class));
        }

        @DisplayName("기존 포인트가 있는 유저가 충전하면 기존 포인트에 누적된다.")
        @Test
        void accumulatesPoint_whenUserAlreadyHasPoint() {
            // arrange
            SignupCommand signupCommand = new SignupCommand(
                    "testId123",
                    "test@test.com",
                    "2000-03-29",
                    "F"
            );
            userService.signup(signupCommand);

            PointCommand.ChargeCommand firstCommand = new PointCommand.ChargeCommand("testId123", 1000);
            pointFacade.chargePoint(firstCommand);

            // act
            PointCommand.ChargeCommand secondCommand = new PointCommand.ChargeCommand("testId123", 500);
            PointInfo pointInfo = pointFacade.chargePoint(secondCommand);

            // assert
            assertThat(pointInfo.totalPoint()).isEqualTo(1500);
        }
    }
    
    @DisplayName("포인트를 조회할 때,")
    @Nested
    class GetPoint {

        @DisplayName("해당 ID의 회원이 존재할 경우, 보유 포인트가 반환된다.")
        @Test
        void returnsPoint_whenUserExists() {
            // arrange
            SignupCommand signupCommand = new SignupCommand(
                    "testId123",
                    "test@test.com",
                    "2000-03-29",
                    "F"
            );
            userService.signup(signupCommand);

            PointCommand.ChargeCommand chargeCommand = new PointCommand.ChargeCommand("testId123", 1200);
            pointFacade.chargePoint(chargeCommand);

            // act
            PointInfo pointInfo = pointFacade.getPointInfo("testId123");

            // assert
            assertThat(pointInfo.totalPoint()).isEqualTo(1200);
        }

        @DisplayName("해당 ID의 회원이 존재하지 않을 경우, Not Found 예외가 발생한다.")
        @Test
        void throwsException_whenUserDoesNotExist() {
            // arrange
            String nonExistentLoginId = "nonExistent";
            PointCommand.ChargeCommand command = new PointCommand.ChargeCommand(nonExistentLoginId, 1000);

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                pointFacade.chargePoint(command);
            });
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
            verify(userService, times(1)).findUserByLoginId(nonExistentLoginId);
        }
    }
}
